package com.dinogo.member.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.member.entity.Member;
import com.dinogo.member.entity.MemberOAuthAccount;
import com.dinogo.member.entity.MemberRole;
import com.dinogo.member.entity.MemberRoleId;
import com.dinogo.member.entity.Role;
import com.dinogo.member.repository.MemberOAuthAccountRepository;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.member.repository.MemberRoleRepository;
import com.dinogo.member.repository.RoleRepository;

/** Executes OAuth writes in their own transaction so duplicate-key races can be recovered safely. */
@Service
public class GoogleAccountProvisioningService {

    private static final String GOOGLE_PROVIDER = "google";
    private static final String DEFAULT_ROLE_NAME = "buyer";

    private final MemberOAuthAccountRepository oauthAccountRepository;
    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final MemberRoleRepository memberRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public GoogleAccountProvisioningService(
            MemberOAuthAccountRepository oauthAccountRepository,
            MemberRepository memberRepository,
            RoleRepository roleRepository,
            MemberRoleRepository memberRoleRepository,
            PasswordEncoder passwordEncoder) {
        this.oauthAccountRepository = oauthAccountRepository;
        this.memberRepository = memberRepository;
        this.roleRepository = roleRepository;
        this.memberRoleRepository = memberRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Member register(String email, GoogleIdentity identity) {
        Role buyerRole = roleRepository.findByRoleName(DEFAULT_ROLE_NAME)
                .orElseThrow(() -> new IllegalStateException("Default role 'buyer' is not configured"));

        Member member = new Member();
        member.setEmail(email);
        member.setPasswordHash(passwordEncoder.encode(UUID.randomUUID() + UUID.randomUUID().toString()));
        member.setFirstName(namePart(identity.firstName(), "Google"));
        member.setLastName(namePart(identity.lastName(), "會員"));
        Member savedMember = memberRepository.saveAndFlush(member);

        MemberRole memberRole = new MemberRole();
        memberRole.setId(new MemberRoleId(savedMember.getMemberId(), buyerRole.getRoleId()));
        memberRole.setMember(savedMember);
        memberRole.setRole(buyerRole);
        memberRoleRepository.save(memberRole);
        oauthAccountRepository.saveAndFlush(newOAuthAccount(savedMember, identity));
        return savedMember;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void link(Member member, GoogleIdentity identity) {
        oauthAccountRepository.saveAndFlush(newOAuthAccount(member, identity));
    }

    private MemberOAuthAccount newOAuthAccount(Member member, GoogleIdentity identity) {
        MemberOAuthAccount account = new MemberOAuthAccount();
        account.setMember(member);
        account.setProvider(GOOGLE_PROVIDER);
        account.setProviderUserId(identity.subject());
        account.setProviderEmail(identity.email());
        return account;
    }

    private String namePart(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.substring(0, Math.min(trimmed.length(), 50));
    }
}
