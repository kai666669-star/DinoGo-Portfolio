package com.dinogo.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.dinogo.member.dto.GoogleLinkRequest;
import com.dinogo.member.dto.GoogleLoginRequest;
import com.dinogo.member.dto.LoginResponse;
import com.dinogo.member.dto.MemberResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.entity.MemberOAuthAccount;
import com.dinogo.member.repository.MemberOAuthAccountRepository;
import com.dinogo.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class GoogleLoginServiceTest {

    @Mock private GoogleIdentityVerifier googleIdentityVerifier;
    @Mock private MemberOAuthAccountRepository oauthAccountRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LoginService loginService;
    @Mock private GoogleAccountProvisioningService provisioningService;

    @InjectMocks private GoogleLoginService googleLoginService;

    @Test
    void loginReturnsExistingLinkedMemberSession() {
        GoogleLoginRequest request = new GoogleLoginRequest("id-token");
        GoogleIdentity identity = identity();
        Member member = member(1, "ACTIVE");
        MemberOAuthAccount account = account(member, identity);
        LoginResponse response = response(member);
        when(googleIdentityVerifier.verify(request.credential())).thenReturn(identity);
        when(oauthAccountRepository.findByProviderAndProviderUserId("google", identity.subject()))
                .thenReturn(Optional.of(account));
        when(loginService.createAuthenticatedResponse(member)).thenReturn(response);

        assertThat(googleLoginService.login(request)).isEqualTo(response);
        verify(memberRepository, never()).existsByEmailIgnoreCase(any());
    }

    @Test
    void loginCreatesBuyerAndOAuthMappingForNewGoogleMember() {
        GoogleLoginRequest request = new GoogleLoginRequest("id-token");
        GoogleIdentity identity = identity();
        Member savedMember = member(2, "ACTIVE");
        LoginResponse response = response(savedMember);
        when(googleIdentityVerifier.verify(request.credential())).thenReturn(identity);
        when(oauthAccountRepository.findByProviderAndProviderUserId("google", identity.subject()))
                .thenReturn(Optional.empty());
        when(memberRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(provisioningService.register("new@example.com", identity)).thenReturn(savedMember);
        when(loginService.createAuthenticatedResponse(savedMember)).thenReturn(response);

        assertThat(googleLoginService.login(request)).isEqualTo(response);
        verify(provisioningService).register("new@example.com", identity);
    }

    @Test
    void loginRequiresPasswordLinkForExistingEmail() {
        GoogleLoginRequest request = new GoogleLoginRequest("id-token");
        GoogleIdentity identity = identity();
        when(googleIdentityVerifier.verify(request.credential())).thenReturn(identity);
        when(oauthAccountRepository.findByProviderAndProviderUserId("google", identity.subject()))
                .thenReturn(Optional.empty());
        when(memberRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(true);

        assertThatThrownBy(() -> googleLoginService.login(request))
                .isInstanceOf(GoogleAccountLinkRequiredException.class);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void loginRecoversWhenConcurrentRegistrationAlreadyCreatedOAuthMapping() {
        GoogleLoginRequest request = new GoogleLoginRequest("id-token");
        GoogleIdentity identity = identity();
        Member member = member(3, "ACTIVE");
        LoginResponse response = response(member);
        when(googleIdentityVerifier.verify(request.credential())).thenReturn(identity);
        when(oauthAccountRepository.findByProviderAndProviderUserId("google", identity.subject()))
                .thenReturn(Optional.empty(), Optional.of(account(member, identity)));
        when(memberRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(provisioningService.register("new@example.com", identity))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(loginService.createAuthenticatedResponse(member)).thenReturn(response);

        assertThat(googleLoginService.login(request)).isEqualTo(response);
    }

    @Test
    void linkRejectsIncorrectPassword() {
        GoogleLinkRequest request = new GoogleLinkRequest("id-token", "wrong-password");
        GoogleIdentity identity = identity();
        Member member = member(1, "ACTIVE");
        when(googleIdentityVerifier.verify(request.credential())).thenReturn(identity);
        when(memberRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(request.password(), member.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> googleLoginService.link(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email 或密碼錯誤");
        verify(oauthAccountRepository, never()).save(any());
    }

    private GoogleIdentity identity() {
        return new GoogleIdentity("google-subject", "New@Example.com", "小明", "王");
    }

    private Member member(int id, String status) {
        Member member = new Member();
        member.setMemberId(id);
        member.setEmail("new@example.com");
        member.setPasswordHash("hash");
        member.setFirstName("小明");
        member.setLastName("王");
        member.setStatus(status);
        return member;
    }

    private MemberOAuthAccount account(Member member, GoogleIdentity identity) {
        MemberOAuthAccount account = new MemberOAuthAccount();
        account.setMember(member);
        account.setProvider("google");
        account.setProviderUserId(identity.subject());
        return account;
    }

    private LoginResponse response(Member member) {
        return new LoginResponse("jwt", MemberResponse.from(member), List.of("buyer"), null);
    }
}
