package com.dinogo.sysmsg.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.SendFailedException;

/** Submits pending Email channels to the configured SMTP provider. */
@Service
@ConditionalOnProperty(name = "sysmsg.email.enabled", havingValue = "true")
public class EmailChannelDispatcher {

    private static final Logger LOG = Logger.getLogger(EmailChannelDispatcher.class.getName());

    private final EmailChannelStateService stateService;
    private final JavaMailSender mailSender;
    private final Clock clock;
    private final int batchSize;
    private final TaskScheduler taskScheduler;
    private final Duration retryDelay;
    private final int maxAttempts;
    /**
     * 功能：避免同一應用實例替同一筆 RecordChannel 同時保留多個重試工作。
     * 應用：初次發送或重試失敗並行發生時，仍只排定一個一分鐘後的單筆重試。
     */
    private final Set<Integer> scheduledRetryIds = ConcurrentHashMap.newKeySet();

    @Autowired
    public EmailChannelDispatcher(
            EmailChannelStateService stateService,
            JavaMailSender mailSender,
            TaskScheduler taskScheduler,
            @Value("${sysmsg.email.batch-size:50}") int batchSize,
            @Value("${sysmsg.email.retry-delay-ms:60000}") long retryDelayMs,
            @Value("${sysmsg.email.max-attempts:5}") int maxAttempts) {
        this(stateService, mailSender, taskScheduler, Clock.systemDefaultZone(),
                batchSize, retryDelayMs, maxAttempts);
    }

    EmailChannelDispatcher(
            EmailChannelStateService stateService,
            JavaMailSender mailSender,
            TaskScheduler taskScheduler,
            Clock clock,
            int batchSize,
            long retryDelayMs,
            int maxAttempts) {
        this.stateService = stateService;
        this.mailSender = mailSender;
        this.taskScheduler = taskScheduler;
        this.clock = clock;
        this.batchSize = Math.max(1, batchSize);
        this.retryDelay = Duration.ofMillis(Math.max(1, retryDelayMs));
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Scheduled(fixedDelayString = "${sysmsg.email.poll-delay-ms:30000}")
    public void dispatchPendingBatch() {
        for (Integer recordChannelId : stateService.findPendingEmailChannelIds(batchSize)) {
            dispatchOne(recordChannelId);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        for (Integer recordChannelId : stateService.findRetryableEmailChannelIds(now, batchSize)) {
            if (!scheduledRetryIds.contains(recordChannelId)) {
                retryOne(recordChannelId);
            }
        }
    }

    void dispatchOne(Integer recordChannelId) {
        boolean submittedToProvider = false;
        try {
            LocalDateTime attemptedAt = LocalDateTime.now(clock);
            if (!stateService.startAttempt(recordChannelId, attemptedAt, false)) {
                return;
            }
            EmailDeliveryCommand command = stateService.prepare(recordChannelId);
            MimeMessage message = createMessage(command);
            String providerMessageId = message.getMessageID();
            if (providerMessageId == null || providerMessageId.isBlank()) {
                throw new IllegalStateException("Email does not have a Message-ID");
            }

            mailSender.send(message);
            submittedToProvider = true;
            stateService.markSent(
                    recordChannelId,
                    providerMessageId,
                    LocalDateTime.now(clock));
        } catch (Exception exception) {
            if (submittedToProvider) {
                // The provider has already accepted the message. A later database
                // failure must not be recorded as an Email delivery failure.
                LOG.log(Level.SEVERE,
                        "Email was submitted but sent_at could not be saved for RecordChannel "
                                + recordChannelId,
                        exception);
                markPersistenceUnknown(recordChannelId, exception);
            } else {
                if (markFailed(recordChannelId, exception, false)) {
                    scheduleRetry(recordChannelId);
                }
            }
        }
    }

    /** 只重試指定的失敗 RecordChannel，不掃描批次、不重建 Record、不檢查訂閱偏好。 */
    void retryOne(Integer recordChannelId) {
        boolean submittedToProvider = false;
        try {
            LocalDateTime attemptedAt = LocalDateTime.now(clock);
            if (!stateService.startAttempt(recordChannelId, attemptedAt, true)) {
                return;
            }
            EmailDeliveryCommand command = stateService.prepareRetry(recordChannelId, attemptedAt);
            MimeMessage message = createMessage(command);
            String providerMessageId = message.getMessageID();
            if (providerMessageId == null || providerMessageId.isBlank()) {
                throw new IllegalStateException("Email does not have a Message-ID");
            }

            mailSender.send(message);
            submittedToProvider = true;
            stateService.markRetrySent(
                    recordChannelId,
                    providerMessageId,
                    LocalDateTime.now(clock));
        } catch (Exception exception) {
            if (submittedToProvider) {
                LOG.log(Level.SEVERE,
                        "Retried Email was submitted but sent_at could not be saved for RecordChannel "
                                + recordChannelId,
                        exception);
                markPersistenceUnknown(recordChannelId, exception);
            } else if (markFailed(recordChannelId, exception, true)) {
                scheduleRetry(recordChannelId);
            }
        }
    }

    private MimeMessage createMessage(EmailDeliveryCommand command) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                false,
                StandardCharsets.UTF_8.name());
        helper.setTo(command.recipientEmail());
        helper.setSubject(command.subject());
        helper.setText(command.content(), false);

        // SMTP send() does not return a Gmail API id. Persist the RFC Message-ID
        // accepted by the provider so sent_at and the trace id are still atomic.
        message.saveChanges();
        return message;
    }

    private boolean markFailed(Integer recordChannelId, Exception exception, boolean retry) {
        String message = exception.getMessage();
        String errorMessage = exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
        try {
            LocalDateTime attemptedAt = LocalDateTime.now(clock);
            FailureClassification classification = classify(exception);
            LocalDateTime nextRetryAt = classification.retryable()
                    ? attemptedAt.plus(retryDelay)
                    : null;
            int allowedAttempts = classification.retryable() ? maxAttempts : 1;
            boolean failureRecorded = retry
                    ? stateService.markRetryFailed(
                            recordChannelId,
                            errorMessage,
                            classification.code(),
                            attemptedAt,
                            nextRetryAt,
                            allowedAttempts)
                    : stateService.markFailed(
                            recordChannelId,
                            errorMessage,
                            classification.code(),
                            attemptedAt,
                            nextRetryAt,
                            allowedAttempts);
            if (!failureRecorded) {
                return false;
            }
        } catch (Exception stateException) {
            LOG.log(Level.SEVERE,
                    "Unable to save Email failure for RecordChannel " + recordChannelId,
                    stateException);
            return false;
        }
        LOG.log(Level.WARNING,
                "Email delivery failed for RecordChannel " + recordChannelId,
                exception);
        return true;
    }

    /** 穩定分類失敗，只有明確的暫時 SMTP 錯誤允許自動重試。 */
    private FailureClassification classify(Exception exception) {
        if (exception instanceof EmailDeliveryPreparationException preparation) {
            return new FailureClassification(preparation.getFailureCode(), false);
        }
        if (exception instanceof MailAuthenticationException) {
            return new FailureClassification("PROVIDER_REJECTED", false);
        }
        if (exception instanceof MailParseException || exception instanceof MailPreparationException) {
            return new FailureClassification("MESSAGE_INVALID", false);
        }
        if (exception instanceof MailSendException sendException
                && sendException.getFailedMessages().values().stream()
                        .anyMatch(this::containsInvalidAddress)) {
            return new FailureClassification("PROVIDER_REJECTED", false);
        }
        if (exception instanceof org.springframework.mail.MailException) {
            return new FailureClassification("PROVIDER_TEMPORARY", true);
        }
        return new FailureClassification("DELIVERY_FAILED", false);
    }

    private boolean containsInvalidAddress(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SendFailedException sendFailed
                    && sendFailed.getInvalidAddresses() != null
                    && sendFailed.getInvalidAddresses().length > 0) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private record FailureClassification(String code, boolean retryable) {}

    private void markPersistenceUnknown(Integer recordChannelId, Exception exception) {
        try {
            String detail = "Provider accepted Email but sent state could not be saved: "
                    + exception.getClass().getSimpleName();
            stateService.markPersistenceUnknown(
                    recordChannelId,
                    detail,
                    LocalDateTime.now(clock));
        } catch (Exception stateException) {
            LOG.log(Level.SEVERE,
                    "Unable to dead-letter uncertain Email state for RecordChannel "
                            + recordChannelId,
                    stateException);
        }
    }

    private void scheduleRetry(Integer recordChannelId) {
        if (!scheduledRetryIds.add(recordChannelId)) {
            return;
        }
        try {
            taskScheduler.schedule(() -> {
                scheduledRetryIds.remove(recordChannelId);
                retryOne(recordChannelId);
            }, clock.instant().plus(retryDelay));
        } catch (RuntimeException exception) {
            scheduledRetryIds.remove(recordChannelId);
            throw exception;
        }
    }
}
