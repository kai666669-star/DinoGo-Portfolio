package com.dinogo.member.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.member.dto.LoginRequest;
import com.dinogo.member.dto.LoginResponse;
import com.dinogo.member.dto.MemberResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.entity.MemberRole;
import com.dinogo.member.repository.MemberRoleRepository;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.security.JwtTokenUtil;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

@Service
public class LoginService {

    private final MemberRepository memberRepository;
    private final MemberRoleRepository memberRoleRepository;
    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public LoginService(
            MemberRepository memberRepository,
            MemberRoleRepository memberRoleRepository,
            SellerRepository sellerRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenUtil jwtTokenUtil) {
        this.memberRepository = memberRepository;
        this.memberRoleRepository = memberRoleRepository;
        this.sellerRepository = sellerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new IllegalArgumentException("Email 或密碼錯誤"));

        if (!"ACTIVE".equals(member.getStatus())
                || !passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new IllegalArgumentException("Email 或密碼錯誤");
        }

        return createAuthenticatedResponse(member);
    }

    @Transactional(readOnly = true)
    public LoginResponse createAuthenticatedResponse(Member member) {
        List<String> roles = memberRoleRepository.findByMemberMemberId(member.getMemberId())
                .stream()
                .map(MemberRole::getRole)
                .map(role -> role.getRoleName())
                .sorted()
                .toList();

        String token = jwtTokenUtil.generateToken(
                member.getEmail(), member.getMemberId(), roles, member.getAuthVersion());
        Integer sellerId = sellerRepository.findByMember_MemberId(member.getMemberId())
                .filter(seller -> "ACTIVE".equals(seller.getStatus()))
                .map(Seller::getSellerId)
                .orElse(null);
        return new LoginResponse(token, MemberResponse.from(member), roles, sellerId);
    }
}
