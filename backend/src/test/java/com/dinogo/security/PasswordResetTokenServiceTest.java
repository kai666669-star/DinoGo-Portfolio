package com.dinogo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.dinogo.member.entity.Member;

class PasswordResetTokenServiceTest {

    private static final String SECRET = "password-reset-test-secret-must-be-at-least-32-bytes";

    @Test
    void createsAndParsesPurposeBoundToken() {
        Member member = new Member();
        member.setMemberId(7);
        member.setEmail("member@example.com");
        member.setAuthVersion(3);
        PasswordResetTokenService service = new PasswordResetTokenService(SECRET, 60_000);

        PasswordResetToken token = service.parse(service.create(member));

        assertThat(token).isEqualTo(new PasswordResetToken(7, "member@example.com", 3));
    }

    @Test
    void rejectsMalformedToken() {
        PasswordResetTokenService service = new PasswordResetTokenService(SECRET, 60_000);

        assertThatThrownBy(() -> service.parse("not-a-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("重設連結無效或已過期");
    }
}
