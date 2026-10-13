package com.dinogo.member.service;

import java.util.Locale;
import java.util.NoSuchElementException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.member.dto.MemberResponse;
import com.dinogo.member.dto.MemberUpdateRequest;
import com.dinogo.member.dto.ChangePasswordRequest;
import com.dinogo.member.dto.RegisterRequest;
import com.dinogo.member.dto.RegisterResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.entity.MemberRole;
import com.dinogo.member.entity.MemberRoleId;
import com.dinogo.member.entity.Role;
import com.dinogo.member.repository.MemberRoleRepository;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.member.repository.RoleRepository;

@Service
public class MemberService {

    private static final String DEFAULT_ROLE_NAME = "buyer";
    private static final String SELLER_ROLE_NAME = "seller";

    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final MemberRoleRepository memberRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(
            MemberRepository memberRepository,
            RoleRepository roleRepository,
            MemberRoleRepository memberRoleRepository,
            PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.roleRepository = roleRepository;
        this.memberRoleRepository = memberRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("密碼與確認密碼不一致");
        }
        PasswordPolicy.validate(request.password(), "密碼");

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (memberRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email 已被註冊");
        }

        Role buyerRole = roleRepository.findByRoleName(DEFAULT_ROLE_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "Default role 'buyer' is not configured"));

        Member member = new Member();
        member.setEmail(email);
        member.setPasswordHash(passwordEncoder.encode(request.password()));
        member.setLastName(request.lastName());
        member.setFirstName(request.firstName());
        member.setBirthDate(request.birthDate());
        member.setPhone(request.phone());

        Member savedMember;
        try {
            savedMember = memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("Email 已被註冊");
        }

        MemberRole memberRole = new MemberRole();
        memberRole.setId(new MemberRoleId(savedMember.getMemberId(), buyerRole.getRoleId()));
        memberRole.setMember(savedMember);
        memberRole.setRole(buyerRole);
        memberRoleRepository.save(memberRole);

        return new RegisterResponse(MemberResponse.from(savedMember));
    }

    @Transactional(readOnly = true)
    public MemberResponse getProfile(Integer memberId) {
        // 根據 JWT 的 memberId 查詢會員
        return MemberResponse.from(findMemberById(memberId));
    }

    @Transactional
    public MemberResponse updateProfile(Integer memberId, MemberUpdateRequest request) {
        int updated = memberRepository.updateProfileFields(
                memberId,
                request.lastName(),
                request.firstName(),
                request.birthDate(),
                request.phone(),
                request.emailOrderNotifications(),
                request.emailMarketingNotifications());
        if (updated == 0) {
            throw new IllegalArgumentException("Member not found");
        }
        return MemberResponse.from(findMemberById(memberId));
    }

    @Transactional
    public void changePassword(Integer memberId, ChangePasswordRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("Member not found"));

        if (!passwordEncoder.matches(request.currentPassword(), member.getPasswordHash())) {
            throw new IllegalArgumentException("目前密碼錯誤");
        }
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new IllegalArgumentException("新密碼與確認密碼不一致");
        }
        PasswordPolicy.validate(request.newPassword(), "新密碼");
        if (passwordEncoder.matches(request.newPassword(), member.getPasswordHash())) {
            throw new IllegalArgumentException("新密碼不可與目前密碼相同");
        }

        int updatedRows = memberRepository.changePasswordIfCurrent(
                memberId,
                member.getPasswordHash(),
                member.getAuthVersion(),
                passwordEncoder.encode(request.newPassword()));
        if (updatedRows != 1) {
            throw new IllegalArgumentException("帳號狀態已變更，請重新登入後再試");
        }
    }

    /**
     * 授予會員商家角色。可重複呼叫，不會重複新增 MemberRole。
     *
     * <p>供商家申請核准流程使用；預設 REQUIRED propagation 會加入呼叫端的交易。</p>
     */
    @Transactional
    public void grantSellerRole(Integer memberId) {
        Member member = findMemberById(memberId);
        Role sellerRole = roleRepository.findByRoleName(SELLER_ROLE_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "Role 'seller' is not configured"));

        if (memberRoleRepository.existsByMemberMemberIdAndRoleRoleId(
                memberId, sellerRole.getRoleId())) {
            return;
        }

        MemberRole memberRole = new MemberRole();
        memberRole.setId(new MemberRoleId(memberId, sellerRole.getRoleId()));
        memberRole.setMember(member);
        memberRole.setRole(sellerRole);
        memberRoleRepository.save(memberRole);
    }

    /**
     * 遞增 auth version，使目前會員的既有 JWT 在交易提交後失效。
     *
     * <p>供商家申請核准流程與其他角色／授權異動流程使用。</p>
     */
    @Transactional
    public void increaseAuthVersion(Integer memberId) {
        if (memberRepository.increaseAuthVersion(memberId) == 0) {
            throw new IllegalArgumentException("Member not found");
        }
    }

    // 先保留，其他功能若仍需要 email 可以使用
    // private Member findMemberByEmail(String email) {
    // return memberRepository.findByEmail(email)
    // .orElseThrow(() -> new IllegalArgumentException("Member not found"));
    // }

    private Member findMemberById(Integer memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
    }

}
