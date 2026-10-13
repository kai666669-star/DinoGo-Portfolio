package com.dinogo.member.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.dinogo.member.dto.PasswordResetRequest;
import com.dinogo.member.dto.ResetPasswordRequest;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.security.PasswordResetToken;
import com.dinogo.security.PasswordResetTokenService;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordResetTokenService passwordResetTokenService;

    @Mock
    private PasswordResetMailService passwordResetMailService;

    @Mock
    private PasswordResetRateLimiter passwordResetRateLimiter;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    void requestSendsResetEmailForActiveMember() {
        Member member = member(1, 4);
        when(memberRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(member));
        when(passwordResetTokenService.create(member)).thenReturn("reset-token");

        passwordResetService.requestPasswordReset(new PasswordResetRequest("User@Example.COM"), "127.0.0.1");

        verify(passwordResetRateLimiter).check("user@example.com", "127.0.0.1");
        verify(passwordResetMailService).sendPasswordReset("user@example.com", "reset-token");
    }

    @Test
    void requestDoesNotSendEmailForUnknownOrInactiveMember() {
        when(memberRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        passwordResetService.requestPasswordReset(new PasswordResetRequest("missing@example.com"), "127.0.0.1");

        verify(passwordResetMailService, never()).sendPasswordReset(any(), any());
        verify(passwordResetTokenService, never()).create(any());
    }

    @Test
    void resetChangesPasswordAndInvalidatesExistingTokens() {
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token", "new-password1", "new-password1");
        when(passwordResetTokenService.parse("reset-token"))
                .thenReturn(new PasswordResetToken(1, "user@example.com", 4));
        when(passwordEncoder.encode("new-password1")).thenReturn("new-hash");
        when(memberRepository.resetPasswordIfTokenIsValid(
                1, "user@example.com", 4, "new-hash"))
                .thenReturn(1);

        passwordResetService.resetPassword(request);

        verify(memberRepository).resetPasswordIfTokenIsValid(
                1, "user@example.com", 4, "new-hash");
    }

    @Test
    void resetRejectsTokenAlreadyUsedByAnotherRequest() {
        when(passwordResetTokenService.parse("reset-token"))
                .thenReturn(new PasswordResetToken(1, "user@example.com", 4));
        when(passwordEncoder.encode("new-password1")).thenReturn("new-hash");
        when(memberRepository.resetPasswordIfTokenIsValid(
                eq(1), eq("user@example.com"), eq(4), eq("new-hash")))
                .thenReturn(0);

        assertThatThrownBy(() -> passwordResetService.resetPassword(
                new ResetPasswordRequest("reset-token", "new-password1", "new-password1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("重設連結無效或已過期");
        verify(memberRepository).resetPasswordIfTokenIsValid(
                1, "user@example.com", 4, "new-hash");
    }

    @Test
    void resetRejectsPasswordWithoutEnglishAndNumber() {
        assertThatThrownBy(() -> passwordResetService.resetPassword(
                new ResetPasswordRequest("reset-token", "12345678", "12345678")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("新密碼須包含英文與數字");

        verify(passwordResetTokenService, never()).parse(any());
    }

    private Member member(int memberId, int authVersion) {
        Member member = new Member();
        member.setMemberId(memberId);
        member.setEmail("user@example.com");
        member.setPasswordHash("old-hash");
        member.setStatus("ACTIVE");
        member.setAuthVersion(authVersion);
        return member;
    }
}
