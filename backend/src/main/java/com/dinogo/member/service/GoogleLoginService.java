package com.dinogo.member.service;

import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.dinogo.member.dto.GoogleLinkRequest;
import com.dinogo.member.dto.GoogleLoginRequest;
import com.dinogo.member.dto.LoginResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.entity.MemberOAuthAccount;
import com.dinogo.member.repository.MemberOAuthAccountRepository;
import com.dinogo.member.repository.MemberRepository;

@Service
public class GoogleLoginService {

    private static final String GOOGLE_PROVIDER = "google";
    private final GoogleIdentityVerifier googleIdentityVerifier;
    private final MemberOAuthAccountRepository oauthAccountRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginService loginService;
    private final GoogleAccountProvisioningService provisioningService;

    public GoogleLoginService(
            GoogleIdentityVerifier googleIdentityVerifier,
            MemberOAuthAccountRepository oauthAccountRepository,
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            LoginService loginService,
            GoogleAccountProvisioningService provisioningService) {
        this.googleIdentityVerifier = googleIdentityVerifier;
        this.oauthAccountRepository = oauthAccountRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginService = loginService;
        this.provisioningService = provisioningService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(GoogleLoginRequest request) {
        GoogleIdentity identity = googleIdentityVerifier.verify(request.credential());
        return oauthAccountRepository.findByProviderAndProviderUserId(GOOGLE_PROVIDER, identity.subject())
                .map(MemberOAuthAccount::getMember)
                .map(this::loginExistingMember)
                .orElseGet(() -> loginOrRegister(identity));
    }

    @Transactional(readOnly = true)
    public LoginResponse link(GoogleLinkRequest request) {
        GoogleIdentity identity = googleIdentityVerifier.verify(request.credential());
        Member member = memberRepository.findByEmailIgnoreCase(normalizeEmail(identity.email()))
                .orElseThrow(() -> new IllegalArgumentException("找不到可綁定的密碼帳號"));

        if (!"ACTIVE".equals(member.getStatus())
                || !passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new IllegalArgumentException("Email 或密碼錯誤");
        }

        MemberOAuthAccount account = oauthAccountRepository
                .findByProviderAndProviderUserId(GOOGLE_PROVIDER, identity.subject())
                .orElse(null);
        if (account != null && !account.getMember().getMemberId().equals(member.getMemberId())) {
            throw new IllegalStateException("此 Google 帳號已綁定其他會員");
        }
        if (account == null) {
            try {
                provisioningService.link(member, identity);
            } catch (DataIntegrityViolationException exception) {
                account = oauthAccountRepository
                        .findByProviderAndProviderUserId(GOOGLE_PROVIDER, identity.subject())
                        .orElseThrow(() -> exception);
                if (!account.getMember().getMemberId().equals(member.getMemberId())) {
                    throw new IllegalStateException("此 Google 帳號已綁定其他會員");
                }
            }
        }
        return loginService.createAuthenticatedResponse(member);
    }

    private LoginResponse loginOrRegister(GoogleIdentity identity) {
        String email = normalizeEmail(identity.email());
        if (memberRepository.existsByEmailIgnoreCase(email)) {
            throw new GoogleAccountLinkRequiredException();
        }

        try {
            return loginService.createAuthenticatedResponse(provisioningService.register(email, identity));
        } catch (DataIntegrityViolationException exception) {
            return recoverConcurrentLogin(identity, exception);
        }
    }

    private LoginResponse loginExistingMember(Member member) {
        if (!"ACTIVE".equals(member.getStatus())) {
            throw new IllegalArgumentException("Google 帳號無法登入");
        }
        return loginService.createAuthenticatedResponse(member);
    }

    private LoginResponse recoverConcurrentLogin(GoogleIdentity identity, DataIntegrityViolationException exception) {
        return oauthAccountRepository.findByProviderAndProviderUserId(GOOGLE_PROVIDER, identity.subject())
                .map(MemberOAuthAccount::getMember)
                .map(this::loginExistingMember)
                .orElseGet(() -> {
                    if (memberRepository.existsByEmailIgnoreCase(normalizeEmail(identity.email()))) {
                        throw new GoogleAccountLinkRequiredException();
                    }
                    throw exception;
                });
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Google 登入憑證無效");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

}
