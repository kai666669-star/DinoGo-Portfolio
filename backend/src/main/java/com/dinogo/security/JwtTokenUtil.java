package com.dinogo.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenUtil {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtTokenUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT_SECRET is required");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
        }
        if (expirationMs <= 0) {
            throw new IllegalStateException("JWT_EXPIRATION_MS must be greater than zero");
        }

        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email, Integer memberId, List<String> roles, int authVersion) {
        Instant issuedAt = Instant.now();
        var builder = Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusMillis(expirationMs)));

        if (memberId != null) {
            builder.claim("memberId", memberId);
        }
        builder.claim("roles", roles == null ? List.of() : List.copyOf(roles));
        builder.claim("authVersion", authVersion);

        return builder.signWith(signingKey).compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public Integer extractMemberId(String token) {
        return parseClaims(token).get("memberId", Integer.class);
    }

    public List<String> extractRoles(String token) {
        Object rolesClaim = parseClaims(token).get("roles");
        if (rolesClaim == null) {
            return List.of();
        }
        if (!(rolesClaim instanceof List<?> roles)) {
            throw new IllegalArgumentException("JWT roles claim must be an array");
        }

        return roles.stream()
                .map(this::requireRoleName)
                .toList();
    }

    public int extractAuthVersion(String token) {
        Integer authVersion = parseClaims(token).get("authVersion", Integer.class);
        return authVersion == null ? 0 : authVersion;
    }

    public boolean isValid(String token) {
        try {
            return StringUtils.hasText(extractSubject(token));
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    private String requireRoleName(Object role) {
        if (!(role instanceof String roleName) || !StringUtils.hasText(roleName)) {
            throw new IllegalArgumentException("JWT roles claim must contain role names");
        }
        return roleName;
    }
}
