package com.dinogo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class JwtTokenUtilTest {

    private static final String SECRET = "test-secret-for-jwt-unit-tests-only-32-bytes";

    private final JwtTokenUtil jwtTokenUtil = new JwtTokenUtil(SECRET, 3_600_000);

    @Test
    void generateTokenContainsMemberIdentityAndRoles() {
        String token = jwtTokenUtil.generateToken("user@example.com", 1, List.of("buyer", "seller"), 3);

        assertThat(jwtTokenUtil.isValid(token)).isTrue();
        assertThat(jwtTokenUtil.extractSubject(token)).isEqualTo("user@example.com");
        assertThat(jwtTokenUtil.parseClaims(token).get("memberId", Integer.class)).isEqualTo(1);
        assertThat(jwtTokenUtil.extractRoles(token)).containsExactly("buyer", "seller");
        assertThat(jwtTokenUtil.extractAuthVersion(token)).isEqualTo(3);
    }

    @Test
    void tokenWithoutRolesClaimUsesEmptyRolesForCompatibility() {
        String token = io.jsonwebtoken.Jwts.builder()
                .subject("user@example.com")
                .claim("memberId", 1)
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtTokenUtil.extractRoles(token)).isEmpty();
    }

    @Test
    void invalidTokenIsRejected() {
        assertThat(jwtTokenUtil.isValid("not-a-jwt")).isFalse();
    }

    @Test
    void secretMustHaveAtLeast32Bytes() {
        assertThatThrownBy(() -> new JwtTokenUtil("short-secret", 3_600_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET must be at least 32 bytes");
    }
}
