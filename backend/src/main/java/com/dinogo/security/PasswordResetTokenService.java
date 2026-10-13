package com.dinogo.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.dinogo.member.entity.Member;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class PasswordResetTokenService {

    private static final String TOKEN_TYPE = "password-reset";

    private final SecretKey signingKey;
    private final long expirationMs;

    public PasswordResetTokenService(
            @Value("${app.password-reset.secret}") String secret,
            @Value("${app.password-reset.expiration-ms:900000}") long expirationMs) {
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("PASSWORD_RESET_SECRET must be at least 32 bytes");
        }
        if (expirationMs <= 0) {
            throw new IllegalStateException("PASSWORD_RESET_EXPIRATION_MS must be greater than zero");
        }

        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String create(Member member) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(member.getEmail())
                .claim("type", TOKEN_TYPE)
                .claim("memberId", member.getMemberId())
                .claim("authVersion", member.getAuthVersion())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    public PasswordResetToken parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Integer memberId = claims.get("memberId", Integer.class);
            Integer authVersion = claims.get("authVersion", Integer.class);

            if (!TOKEN_TYPE.equals(claims.get("type", String.class))
                    || memberId == null
                    || authVersion == null
                    || !StringUtils.hasText(claims.getSubject())) {
                throw invalidToken();
            }
            return new PasswordResetToken(memberId, claims.getSubject(), authVersion);
        } catch (JwtException | IllegalArgumentException exception) {
            throw invalidToken();
        }
    }

    private IllegalArgumentException invalidToken() {
        return new IllegalArgumentException("重設連結無效或已過期");
    }
}
