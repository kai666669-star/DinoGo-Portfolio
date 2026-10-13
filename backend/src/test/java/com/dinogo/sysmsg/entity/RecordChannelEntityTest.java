package com.dinogo.sysmsg.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class RecordChannelEntityTest {
    @Test
    void pendingChannelCanBeMarkedSent() {
        RecordChannelEntity channel = channel();
        LocalDateTime sentAt = LocalDateTime.of(2026, 8, 23, 12, 0);

        channel.startAttempt(sentAt);
        channel.markSent("message-id", sentAt);

        assertTrue(channel.isSent());
        assertEquals(sentAt, channel.getSentAt());
        assertEquals("message-id", channel.getProviderMessageId());
        assertNull(channel.getErrorMessage());
    }

    @Test
    void failedChannelCanRetryAndClearPreviousError() {
        RecordChannelEntity channel = channel();
        LocalDateTime failedAt = LocalDateTime.of(2026, 8, 23, 12, 0);
        channel.startAttempt(failedAt);
        channel.markFailed(
                "SMTP unavailable",
                "PROVIDER_TEMPORARY",
                failedAt,
                failedAt.plusMinutes(1),
                5);
        assertTrue(channel.isFailed());
        assertEquals(1, channel.getAttemptCount());
        assertEquals(failedAt.plusMinutes(1), channel.getNextRetryAt());

        LocalDateTime retriedAt = LocalDateTime.of(2026, 8, 23, 12, 1);
        channel.startAttempt(retriedAt);
        channel.markSent("retry-message-id", retriedAt);

        assertTrue(channel.isSent());
        assertFalse(channel.isFailed());
        assertNull(channel.getErrorMessage());
        assertNull(channel.getFailureCode());
        assertNull(channel.getNextRetryAt());
        assertEquals(2, channel.getAttemptCount());
    }

    @Test
    void maximumAttemptMovesSameChannelToDeadLetter() {
        RecordChannelEntity channel = channel();
        LocalDateTime attemptedAt = LocalDateTime.of(2026, 8, 23, 12, 0);

        channel.startAttempt(attemptedAt);
        channel.markFailed(
                "Rejected",
                "PROVIDER_REJECTED",
                attemptedAt,
                attemptedAt.plusMinutes(1),
                1);

        assertTrue(channel.isDeadLettered());
        assertEquals(attemptedAt, channel.getDeadLetteredAt());
        assertNull(channel.getNextRetryAt());
    }

    @Test
    void sentStateRequiresTimestampAndProviderMessageId() {
        RecordChannelEntity channel = channel();

        channel.startAttempt(LocalDateTime.now());
        assertThrows(IllegalArgumentException.class,
                () -> channel.markSent(null, LocalDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> channel.markSent("message-id", null));
    }

    private RecordChannelEntity channel() {
        return new RecordChannelEntity(10, 20, ChannelType.EMAIL, NotificationType.ORDER);
    }
}
