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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.dinogo.member.dto.LoginRequest;
import com.dinogo.member.dto.LoginResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.entity.MemberRole;
import com.dinogo.member.entity.Role;
import com.dinogo.member.repository.MemberRoleRepository;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.security.JwtTokenUtil;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberRoleRepository memberRoleRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @InjectMocks
    private LoginService loginService;

    @Test
    void loginReturnsMemberWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        Member member = member("$2a$hashed-password", "ACTIVE");
        List<String> roles = List.of("buyer", "seller");
        when(memberRepository.findByEmailIgnoreCase(request.email())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(request.password(), member.getPasswordHash())).thenReturn(true);
        when(memberRoleRepository.findByMemberMemberId(member.getMemberId()))
                .thenReturn(List.of(memberRole("seller"), memberRole("buyer")));
        when(sellerRepository.findByMember_MemberId(member.getMemberId()))
                .thenReturn(Optional.of(seller(7, "ACTIVE")));
        when(jwtTokenUtil.generateToken(member.getEmail(), member.getMemberId(), roles, 0))
                .thenReturn("jwt-token");

        LoginResponse response = loginService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.member().email()).isEqualTo(request.email());
        assertThat(response.roles()).containsExactlyElementsOf(roles);
        assertThat(response.sellerId()).isEqualTo(7);
        verify(jwtTokenUtil).generateToken(member.getEmail(), member.getMemberId(), roles, 0);
    }

    @Test
    void loginReturnsNullSellerIdWhenMemberHasNoActiveSeller() {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        Member member = member("$2a$hashed-password", "ACTIVE");
        List<String> roles = List.of("buyer");
        when(memberRepository.findByEmailIgnoreCase(request.email())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(request.password(), member.getPasswordHash())).thenReturn(true);
        when(memberRoleRepository.findByMemberMemberId(member.getMemberId()))
                .thenReturn(List.of(memberRole("buyer")));
        when(sellerRepository.findByMember_MemberId(member.getMemberId()))
                .thenReturn(Optional.of(seller(7, "PENDING")));
        when(jwtTokenUtil.generateToken(member.getEmail(), member.getMemberId(), roles, 0))
                .thenReturn("jwt-token");

        LoginResponse response = loginService.login(request);

        assertThat(response.sellerId()).isNull();
    }

    @Test
    void loginRejectsInvalidPassword() {
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");
        Member member = member("$2a$hashed-password", "ACTIVE");
        when(memberRepository.findByEmailIgnoreCase(request.email())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(request.password(), member.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> loginService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email 或密碼錯誤");
        verify(memberRoleRepository, never()).findByMemberMemberId(any());
    }

    @Test
    void loginRejectsMissingOrInactiveMember() {
        LoginRequest missingRequest = new LoginRequest("missing@example.com", "password123");
        when(memberRepository.findByEmailIgnoreCase(missingRequest.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.login(missingRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email 或密碼錯誤");

        LoginRequest inactiveRequest = new LoginRequest("inactive@example.com", "password123");
        Member inactiveMember = member("$2a$hashed-password", "INACTIVE");
        when(memberRepository.findByEmailIgnoreCase(inactiveRequest.email())).thenReturn(Optional.of(inactiveMember));

        assertThatThrownBy(() -> loginService.login(inactiveRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email 或密碼錯誤");
        verify(passwordEncoder, never()).matches(any(), any());
        verify(memberRoleRepository, never()).findByMemberMemberId(any());
    }

    private Member member(String passwordHash, String status) {
        Member member = new Member();
        member.setMemberId(1);
        member.setEmail("user@example.com");
        member.setPasswordHash(passwordHash);
        member.setLastName("王");
        member.setFirstName("小明");
        member.setStatus(status);
        return member;
    }

    private MemberRole memberRole(String roleName) {
        Role role = new Role();
        role.setRoleName(roleName);
        MemberRole memberRole = new MemberRole();
        memberRole.setRole(role);
        return memberRole;
    }

    private Seller seller(int sellerId, String status) {
        Seller seller = new Seller();
        seller.setSellerId(sellerId);
        seller.setStatus(status);
        return seller;
    }
}
