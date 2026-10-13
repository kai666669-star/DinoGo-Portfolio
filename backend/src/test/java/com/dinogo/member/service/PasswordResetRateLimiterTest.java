package com.dinogo.member.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class PasswordResetRateLimiterTest {

    @Test
    void limitsRepeatedRequestsByEmail() {
        MutableClock clock = new MutableClock();
        PasswordResetRateLimiter limiter = new PasswordResetRateLimiter(60_000, 10, clock);

        assertThatCode(() -> limiter.check("member@example.com", "127.0.0.1"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.check("member@example.com", "127.0.0.2"))
                .isInstanceOf(PasswordResetRateLimitException.class);
    }

    @Test
    void limitsRepeatedRequestsByClientIp() {
        MutableClock clock = new MutableClock();
        PasswordResetRateLimiter limiter = new PasswordResetRateLimiter(60_000, 10, clock);

        limiter.check("first@example.com", "127.0.0.1");

        assertThatThrownBy(() -> limiter.check("second@example.com", "127.0.0.1"))
                .isInstanceOf(PasswordResetRateLimitException.class);
    }

    @Test
    void removesExpiredKeysBeforeCheckingTheMaximumCapacity() {
        MutableClock clock = new MutableClock();
        PasswordResetRateLimiter limiter = new PasswordResetRateLimiter(60_000, 2, clock);
        limiter.check("member@example.com", "127.0.0.1");

        clock.advanceMillis(60_000);

        assertThatCode(() -> limiter.check("next@example.com", "127.0.0.2"))
                .doesNotThrowAnyException();
    }

    private static final class MutableClock extends Clock {

        private Instant instant = Instant.parse("2026-08-24T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advanceMillis(long millis) {
            instant = instant.plusMillis(millis);
        }
    }
}
