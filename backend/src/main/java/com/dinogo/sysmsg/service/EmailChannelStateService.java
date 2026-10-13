package com.dinogo.sysmsg.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.member.service.MemberSysmsgProviderService;
import com.dinogo.seller.service.SellerSysmsgProviderService;
import com.dinogo.sysmsg.entity.ChannelType;
import com.dinogo.sysmsg.entity.RecordChannelEntity;
import com.dinogo.sysmsg.entity.RecordEntity;
import com.dinogo.sysmsg.repository.RecordChannelRepository;
import com.dinogo.sysmsg.repository.RecordRepository;

/** Transactional database boundary for Email delivery. */
@Service
public class EmailChannelStateService {

    private final RecordChannelRepository channelRepository;
    private final RecordRepository recordRepository;
    private final MemberSysmsgProviderService memberProvider;
    private final SellerSysmsgProviderService sellerProvider;

    public EmailChannelStateService(
            RecordChannelRepository channelRepository,
            RecordRepository recordRepository,
            MemberSysmsgProviderService memberProvider,
            SellerSysmsgProviderService sellerProvider) {
        this.channelRepository = channelRepository;
        this.recordRepository = recordRepository;
        this.memberProvider = memberProvider;
        this.sellerProvider = sellerProvider;
    }

    @Transactional(readOnly = true)
    public List<Integer> findPendingEmailChannelIds(int batchSize) {
        int safeBatchSize = Math.max(1, batchSize);
        return channelRepository
                .findByChannelTypeAndSentAtIsNullAndProviderMessageIdIsNullAndErrorMessageIsNullAndAttemptCountAndLastAttemptAtIsNullOrderByRecordChannelIdAsc(
                        ChannelType.EMAIL,
                        0,
                        PageRequest.of(0, safeBatchSize))
                .map(RecordChannelEntity::getRecordChannelId)
                .getContent();
    }

    @Transactional(readOnly = true)
    public List<Integer> findRetryableEmailChannelIds(
            LocalDateTime now,
            int batchSize) {
        int safeBatchSize = Math.max(1, batchSize);
        return channelRepository
                .findByChannelTypeAndSentAtIsNullAndProviderMessageIdIsNullAndErrorMessageIsNotNullAndNextRetryAtLessThanEqualAndDeadLetteredAtIsNullOrderByNextRetryAtAscRecordChannelIdAsc(
                        ChannelType.EMAIL,
                        now,
                        PageRequest.of(0, safeBatchSize))
                .map(RecordChannelEntity::getRecordChannelId)
                .getContent();
    }

    @Transactional(readOnly = true)
    public EmailDeliveryCommand prepare(Integer recordChannelId) {
        RecordChannelEntity channel = requireEmailChannel(recordChannelId);
        if (!channel.isAttemptInProgress()) {
            throw new IllegalStateException("Email channel attempt is not active: " + recordChannelId);
        }

        return prepareDelivery(channel);
    }

    /**
     * 只重新檢查指定的失敗 RecordChannel；不重建 Record，也不重新驗證訂閱偏好。
     * 收件地址仍依原 Record 的收件 ID 取得最新值。
     */
    @Transactional(readOnly = true)
    public EmailDeliveryCommand prepareRetry(
            Integer recordChannelId,
            LocalDateTime now) {
        RecordChannelEntity channel = requireEmailChannel(recordChannelId);
        if (!channel.isAttemptInProgress()) {
            throw new IllegalStateException("Email retry attempt is not active: " + recordChannelId);
        }

        return prepareDelivery(channel);
    }

    @Transactional
    public boolean startAttempt(
            Integer recordChannelId,
            LocalDateTime attemptedAt,
            boolean retry) {
        RecordChannelEntity channel = requireEmailChannelForUpdate(recordChannelId);
        boolean eligible = retry
                ? channel.isRetryableAt(attemptedAt)
                : channel.isPending();
        if (!eligible) {
            return false;
        }
        channel.startAttempt(attemptedAt);
        return true;
    }

    private EmailDeliveryCommand prepareDelivery(RecordChannelEntity channel) {

        RecordEntity record = recordRepository.findWithSendByRecordId(channel.getRecordId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Record not found for Email channel: " + channel.getRecordId()));

        if (!channel.getSendId().equals(record.getSend().getSendId())) {
            throw new IllegalStateException("Email channel does not match its Record and Send");
        }

        String recipientEmail = resolveRecipientEmail(record);
        return new EmailDeliveryCommand(
                channel.getRecordChannelId(),
                requireText(recipientEmail, "ADDRESS_MISSING", "Recipient Email is missing"),
                requireText(record.getSend().getSendTitle(), "MESSAGE_INVALID", "Email subject is missing"),
                requireText(record.getSend().getSendContent(), "MESSAGE_INVALID", "Email content is missing"));
    }

    @Transactional
    public void markSent(Integer recordChannelId, String providerMessageId, LocalDateTime sentAt) {
        RecordChannelEntity channel = requireEmailChannelForUpdate(recordChannelId);
        if (!channel.isAttemptInProgress()) {
            return;
        }
        channel.markSent(providerMessageId, sentAt);
    }

    @Transactional
    public boolean markFailed(
            Integer recordChannelId,
            String errorMessage,
            String failureCode,
            LocalDateTime attemptedAt,
            LocalDateTime nextRetryAt,
            int maxAttempts) {
        RecordChannelEntity channel = requireEmailChannelForUpdate(recordChannelId);
        if (!channel.isAttemptInProgress()) {
            return false;
        }
        channel.markFailed(
                limitErrorMessage(errorMessage),
                limitFailureCode(failureCode),
                attemptedAt,
                nextRetryAt,
                maxAttempts);
        return !channel.isDeadLettered();
    }

    @Transactional
    public void markRetrySent(Integer recordChannelId, String providerMessageId, LocalDateTime sentAt) {
        RecordChannelEntity channel = requireEmailChannelForUpdate(recordChannelId);
        if (!channel.isAttemptInProgress()) {
            return;
        }
        channel.markSent(providerMessageId, sentAt);
    }

    @Transactional
    public boolean markRetryFailed(
            Integer recordChannelId,
            String errorMessage,
            String failureCode,
            LocalDateTime attemptedAt,
            LocalDateTime nextRetryAt,
            int maxAttempts) {
        RecordChannelEntity channel = requireEmailChannelForUpdate(recordChannelId);
        if (!channel.isAttemptInProgress()) {
            return false;
        }
        channel.markFailed(
                limitErrorMessage(errorMessage),
                limitFailureCode(failureCode),
                attemptedAt,
                nextRetryAt,
                maxAttempts);
        return !channel.isDeadLettered();
    }

    /** Provider 已接受但成功狀態保存失敗時，盡力將原 Channel 標成不可重送死信。 */
    @Transactional
    public void markPersistenceUnknown(
            Integer recordChannelId,
            String errorMessage,
            LocalDateTime failedAt) {
        RecordChannelEntity channel = requireEmailChannelForUpdate(recordChannelId);
        if (!channel.isAttemptInProgress()) {
            return;
        }
        channel.markFailed(
                limitErrorMessage(errorMessage),
                "STATE_PERSISTENCE_UNKNOWN",
                failedAt,
                null,
                1);
    }

    private RecordChannelEntity requireEmailChannel(Integer recordChannelId) {
        RecordChannelEntity channel = channelRepository.findById(recordChannelId)
                .orElseThrow(() -> new NoSuchElementException(
                        "RecordChannel not found: " + recordChannelId));
        if (channel.getChannelType() != ChannelType.EMAIL) {
            throw new IllegalArgumentException("RecordChannel is not an Email channel");
        }
        return channel;
    }

    private RecordChannelEntity requireEmailChannelForUpdate(Integer recordChannelId) {
        RecordChannelEntity channel = channelRepository.findByIdForUpdate(recordChannelId)
                .orElseThrow(() -> new NoSuchElementException(
                        "RecordChannel not found: " + recordChannelId));
        if (channel.getChannelType() != ChannelType.EMAIL) {
            throw new IllegalArgumentException("RecordChannel is not an Email channel");
        }
        return channel;
    }

    private String resolveRecipientEmail(RecordEntity record) {
        if (record.getMsgtoMemberId() != null) {
            return memberProvider.getMember(record.getMsgtoMemberId()).email();
        }
        if (record.getMsgtoSellerId() != null) {
            return sellerProvider.getSeller(record.getMsgtoSellerId()).email();
        }
        throw new IllegalStateException("Record does not have a recipient");
    }

    private String requireText(String value, String failureCode, String message) {
        if (value == null || value.isBlank()) {
            throw new EmailDeliveryPreparationException(failureCode, message);
        }
        return value.trim();
    }

    private String limitErrorMessage(String errorMessage) {
        String value = errorMessage == null || errorMessage.isBlank()
                ? "Unknown Email delivery failure"
                : errorMessage.trim();
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }

    private String limitFailureCode(String failureCode) {
        String value = failureCode == null || failureCode.isBlank()
                ? "DELIVERY_FAILED"
                : failureCode.trim().toUpperCase();
        return value.length() <= 50 ? value : value.substring(0, 50);
    }
}
