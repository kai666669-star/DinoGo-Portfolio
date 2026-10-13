package com.dinogo.member.service;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 單一應用程式節點的密碼重設請求限流。
 * 多節點部署時需以共享的 rate-limit store 取代此實作。
 */
@Component
public class PasswordResetRateLimiter {

    private final Map<String, Long> expiresAtByKey = new HashMap<>();
    private final Deque<ExpiryEntry> expiryQueue = new ArrayDeque<>();
    private final long windowMs;
    private final int maxTrackedKeys;
    private final Clock clock;

    @Autowired
    public PasswordResetRateLimiter(
            @Value("${app.password-reset.rate-limit-window-ms:60000}") long windowMs,
            @Value("${app.password-reset.rate-limit-max-keys:10000}") int maxTrackedKeys) {
        this(windowMs, maxTrackedKeys, Clock.systemUTC());
    }

    PasswordResetRateLimiter(long windowMs, int maxTrackedKeys, Clock clock) {
        if (windowMs <= 0) {
            throw new IllegalStateException("PASSWORD_RESET_RATE_LIMIT_WINDOW_MS must be greater than zero");
        }
        if (maxTrackedKeys < 2) {
            throw new IllegalStateException("PASSWORD_RESET_RATE_LIMIT_MAX_KEYS must be at least 2");
        }
        this.windowMs = windowMs;
        this.maxTrackedKeys = maxTrackedKeys;
        this.clock = clock;
    }

    public synchronized void check(String email, String clientIp) {
        long now = clock.millis();
        removeExpiredEntries(now);
        String ip = StringUtils.hasText(clientIp) ? clientIp : "unknown";
        String emailKey = "email:" + email;
        String ipKey = "ip:" + ip;
        if (expiresAtByKey.containsKey(emailKey) || expiresAtByKey.containsKey(ipKey)) {
            throw new PasswordResetRateLimitException();
        }
        if (expiresAtByKey.size() + 2 > maxTrackedKeys) {
            throw new PasswordResetRateLimitException();
        }

        long expiresAt = Math.addExact(now, windowMs);
        track(emailKey, expiresAt);
        track(ipKey, expiresAt);
    }

    private void removeExpiredEntries(long now) {
        while (!expiryQueue.isEmpty() && expiryQueue.peekFirst().expiresAt() <= now) {
            ExpiryEntry expired = expiryQueue.removeFirst();
            expiresAtByKey.remove(expired.key(), expired.expiresAt());
        }
    }

    private void track(String key, long expiresAt) {
        expiresAtByKey.put(key, expiresAt);
        expiryQueue.addLast(new ExpiryEntry(key, expiresAt));
    }

    private record ExpiryEntry(String key, long expiresAt) {
    }
}
