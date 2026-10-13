package com.dinogo.sysmsg.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.TaskScheduler;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

class EmailChannelDispatcherTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-23T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void marksSpecifiedChannelSentAfterProviderAcceptsMessage() {
        EmailChannelStateService state = mock(EmailChannelStateService.class);
        JavaMailSender mail = mailSender();
        TaskScheduler scheduler = mock(TaskScheduler.class);
        when(state.startAttempt(org.mockito.ArgumentMatchers.eq(9), any(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(true);
        when(state.prepare(9)).thenReturn(command(9));

        new EmailChannelDispatcher(state, mail, scheduler, CLOCK, 10, 60_000, 5)
                .dispatchOne(9);

        verify(mail).send(any(MimeMessage.class));
        verify(state).markSent(org.mockito.ArgumentMatchers.eq(9), anyString(), any());
    }

    @Test
    void failureIsRecordedAndOnlyThatChannelRetriesAfterOneMinute() {
        EmailChannelStateService state = mock(EmailChannelStateService.class);
        JavaMailSender mail = mailSender();
        TaskScheduler scheduler = mock(TaskScheduler.class);
        when(state.startAttempt(org.mockito.ArgumentMatchers.eq(9), any(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(true);
        when(state.startAttempt(org.mockito.ArgumentMatchers.eq(9), any(), org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(true);
        when(state.prepare(9)).thenReturn(command(9));
        when(state.prepareRetry(org.mockito.ArgumentMatchers.eq(9), any())).thenReturn(command(9));
        when(state.markFailed(
                org.mockito.ArgumentMatchers.eq(9),
                anyString(),
                anyString(),
                any(),
                any(),
                org.mockito.ArgumentMatchers.eq(5))).thenReturn(true);
        doThrow(new MailSendException("down")).doNothing()
                .when(mail).send(any(MimeMessage.class));

        EmailChannelDispatcher dispatcher = new EmailChannelDispatcher(
                state, mail, scheduler, CLOCK, 10, 60_000, 5);
        dispatcher.dispatchOne(9);

        ArgumentCaptor<Runnable> retry = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(retry.capture(),
                org.mockito.ArgumentMatchers.eq(Instant.parse("2026-08-23T00:01:00Z")));
        retry.getValue().run();

        verify(state).prepareRetry(org.mockito.ArgumentMatchers.eq(9), any());
        verify(state).markRetrySent(org.mockito.ArgumentMatchers.eq(9), anyString(), any());
    }

    @Test
    void duplicateFailuresDoNotScheduleDuplicateRetryForSameChannel() {
        EmailChannelStateService state = mock(EmailChannelStateService.class);
        JavaMailSender mail = mailSender();
        TaskScheduler scheduler = mock(TaskScheduler.class);
        when(state.startAttempt(org.mockito.ArgumentMatchers.eq(9), any(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(true);
        when(state.prepare(9)).thenReturn(command(9));
        when(state.markFailed(
                org.mockito.ArgumentMatchers.eq(9),
                anyString(),
                anyString(),
                any(),
                any(),
                org.mockito.ArgumentMatchers.eq(5))).thenReturn(true);
        doThrow(new MailSendException("down")).when(mail).send(any(MimeMessage.class));

        EmailChannelDispatcher dispatcher = new EmailChannelDispatcher(
                state, mail, scheduler, CLOCK, 10, 60_000, 5);
        dispatcher.dispatchOne(9);
        dispatcher.dispatchOne(9);

        verify(scheduler, times(1)).schedule(any(Runnable.class),
                org.mockito.ArgumentMatchers.eq(Instant.parse("2026-08-23T00:01:00Z")));
    }

    @Test
    void missingAddressIsDeadLetteredWithoutSchedulingRetry() {
        EmailChannelStateService state = mock(EmailChannelStateService.class);
        JavaMailSender mail = mailSender();
        TaskScheduler scheduler = mock(TaskScheduler.class);
        when(state.startAttempt(org.mockito.ArgumentMatchers.eq(9), any(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(true);
        when(state.prepare(9)).thenThrow(
                new EmailDeliveryPreparationException("ADDRESS_MISSING", "Recipient Email is missing"));
        when(state.markFailed(
                org.mockito.ArgumentMatchers.eq(9), anyString(),
                org.mockito.ArgumentMatchers.eq("ADDRESS_MISSING"), any(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(1)))
                .thenReturn(false);

        new EmailChannelDispatcher(state, mail, scheduler, CLOCK, 10, 60_000, 5)
                .dispatchOne(9);

        verify(state).markFailed(org.mockito.ArgumentMatchers.eq(9), anyString(),
                org.mockito.ArgumentMatchers.eq("ADDRESS_MISSING"), any(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(1));
        verify(scheduler, org.mockito.Mockito.never()).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void providerAuthenticationRejectionIsNotRetried() {
        EmailChannelStateService state = mock(EmailChannelStateService.class);
        JavaMailSender mail = mailSender();
        TaskScheduler scheduler = mock(TaskScheduler.class);
        when(state.startAttempt(org.mockito.ArgumentMatchers.eq(9), any(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(true);
        when(state.prepare(9)).thenReturn(command(9));
        doThrow(new MailAuthenticationException("rejected"))
                .when(mail).send(any(MimeMessage.class));
        when(state.markFailed(
                org.mockito.ArgumentMatchers.eq(9), anyString(),
                org.mockito.ArgumentMatchers.eq("PROVIDER_REJECTED"), any(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(1)))
                .thenReturn(false);

        new EmailChannelDispatcher(state, mail, scheduler, CLOCK, 10, 60_000, 5)
                .dispatchOne(9);

        verify(state).markFailed(org.mockito.ArgumentMatchers.eq(9), anyString(),
                org.mockito.ArgumentMatchers.eq("PROVIDER_REJECTED"), any(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(1));
        verify(scheduler, org.mockito.Mockito.never()).schedule(any(Runnable.class), any(Instant.class));
    }

    private JavaMailSender mailSender() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(
                invocation -> new MimeMessage((Session) null));
        return sender;
    }

    private EmailDeliveryCommand command(Integer id) {
        return new EmailDeliveryCommand(id, "member@example.com", "subject", "content");
    }
}
