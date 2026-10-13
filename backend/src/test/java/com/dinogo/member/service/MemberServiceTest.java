package com.dinogo.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private MemberRoleRepository memberRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest(
                "user@example.com",
                "password123",
                "password123",
                "王",
                "小明",
                LocalDate.of(2000, 1, 1),
                "0912345678");
    }

    @Test
    void registerHashesPasswordBeforeSaving() {
        Role buyerRole = buyerRole();
        when(memberRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(roleRepository.findByRoleName("buyer")).thenReturn(Optional.of(buyerRole));
        when(passwordEncoder.encode(request.password())).thenReturn("$2a$hashed-password");
        when(memberRepository.saveAndFlush(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            member.setMemberId(1);
            return member;
        });

        RegisterResponse response = memberService.register(request);

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$hashed-password");
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo(request.password());
        assertThat(response.member().memberId()).isEqualTo(1);

        ArgumentCaptor<MemberRole> memberRoleCaptor = ArgumentCaptor.forClass(MemberRole.class);
        verify(memberRoleRepository).save(memberRoleCaptor.capture());
        MemberRole savedMemberRole = memberRoleCaptor.getValue();
        assertThat(savedMemberRole.getId().getMemberId()).isEqualTo(1);
        assertThat(savedMemberRole.getId().getRoleId()).isEqualTo(1);
        assertThat(savedMemberRole.getMember()).isSameAs(captor.getValue());
        assertThat(savedMemberRole.getRole()).isSameAs(buyerRole);
    }

    @Test
    void registerFailsWhenDefaultBuyerRoleIsMissing() {
        when(memberRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(roleRepository.findByRoleName("buyer")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.register(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Default role 'buyer' is not configured");

        verify(passwordEncoder, never()).encode(any());
        verify(memberRepository, never()).saveAndFlush(any(Member.class));
        verify(memberRoleRepository, never()).save(any(MemberRole.class));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(memberRepository.existsByEmailIgnoreCase(request.email())).thenReturn(true);

        assertThatThrownBy(() -> memberService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email 已被註冊");

        verify(passwordEncoder, never()).encode(any());
        verify(memberRepository, never()).save(any(Member.class));
        verify(roleRepository, never()).findByRoleName(any());
        verify(memberRoleRepository, never()).save(any(MemberRole.class));
    }

    @Test
    void registerNormalizesEmailBeforeCheckingAndSaving() {
        RegisterRequest mixedCaseRequest = new RegisterRequest(
                "User@Example.COM", "password123", "password123", "王", "小明", null, null);
        when(memberRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(roleRepository.findByRoleName("buyer")).thenReturn(Optional.of(buyerRole()));
        when(passwordEncoder.encode(mixedCaseRequest.password())).thenReturn("hashed-password");
        when(memberRepository.saveAndFlush(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            member.setMemberId(1);
            return member;
        });

        memberService.register(mixedCaseRequest);

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void registerRejectsMismatchedPasswords() {
        RegisterRequest mismatchedRequest = new RegisterRequest(
                request.email(), request.password(), "different-password",
                request.lastName(), request.firstName(), request.birthDate(), request.phone());

        assertThatThrownBy(() -> memberService.register(mismatchedRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("密碼與確認密碼不一致");

        verify(memberRepository, never()).existsByEmailIgnoreCase(any());
        verify(passwordEncoder, never()).encode(any());
        verify(memberRepository, never()).saveAndFlush(any(Member.class));
        verify(roleRepository, never()).findByRoleName(any());
        verify(memberRoleRepository, never()).save(any(MemberRole.class));
    }

    @Test
    void getProfileFindsMemberByAuthenticatedMemberId() {
        Member member = new Member();
        member.setMemberId(1);
        member.setEmail("user@example.com");
        member.setLastName("王");
        member.setFirstName("小明");
        member.setEmailOrderNotifications(true);
        member.setEmailMarketingNotifications(false);
        member.setCreatedAt(LocalDateTime.of(2025, 8, 18, 10, 0));
        member.setUpdatedAt(LocalDateTime.of(2025, 8, 20, 9, 30));
        when(memberRepository.findById(member.getMemberId()))
                .thenReturn(Optional.of(member));

        MemberResponse response = memberService.getProfile(member.getMemberId());

        assertThat(response.memberId()).isEqualTo(1);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2025, 8, 18, 10, 0));
        assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2025, 8, 20, 9, 30));
        // 會員身份由 JWT 的 memberId 決定，Service 應使用 findById 查詢。
        verify(memberRepository).findById(member.getMemberId());
    }

    @Test
    void updateProfileUsesLimitedColumnUpdateAndReturnsFreshMemberState() {
        Member member = new Member();
        member.setMemberId(1);
        member.setEmail("user@example.com");
        member.setPasswordHash("hashed-password");
        member.setLastName("王");
        member.setFirstName("小明");
        member.setCreatedAt(LocalDateTime.of(2025, 8, 18, 10, 0));
        member.setUpdatedAt(LocalDateTime.of(2025, 8, 20, 9, 30));
        MemberUpdateRequest request = new MemberUpdateRequest(
                "林", "小美", LocalDate.of(1998, 2, 3), "0987654321", false, true);
        Member updatedMember = new Member();
        updatedMember.setMemberId(1);
        updatedMember.setEmail("user@example.com");
        updatedMember.setPasswordHash("hashed-password");
        updatedMember.setStatus("SUSPENDED");
        updatedMember.setAuthVersion(5);
        updatedMember.setLastName(request.lastName());
        updatedMember.setFirstName(request.firstName());
        updatedMember.setBirthDate(request.birthDate());
        updatedMember.setPhone(request.phone());
        updatedMember.setEmailOrderNotifications(false);
        updatedMember.setEmailMarketingNotifications(true);
        updatedMember.setCreatedAt(LocalDateTime.of(2025, 8, 18, 10, 0));
        updatedMember.setUpdatedAt(LocalDateTime.of(2026, 8, 14, 15, 10));
        when(memberRepository.updateProfileFields(
                1, "林", "小美", request.birthDate(), "0987654321", false, true)).thenReturn(1);
        when(memberRepository.findById(1)).thenReturn(Optional.of(updatedMember));

        MemberResponse response = memberService.updateProfile(member.getMemberId(), request);

        assertThat(response.lastName()).isEqualTo("林");
        assertThat(response.firstName()).isEqualTo("小美");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1998, 2, 3));
        assertThat(response.phone()).isEqualTo("0987654321");
        assertThat(response.emailOrderNotifications()).isFalse();
        assertThat(response.emailMarketingNotifications()).isTrue();
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2025, 8, 18, 10, 0));
        assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 14, 15, 10));
        assertThat(response.status()).isEqualTo("SUSPENDED");
        verify(memberRepository).updateProfileFields(
                1, "林", "小美", request.birthDate(), "0987654321", false, true);
        verify(memberRepository).findById(1);
    }

    @Test
    void changePasswordHashesNewPasswordAndInvalidatesExistingTokens() {
        Member member = new Member();
        member.setMemberId(1);
        member.setPasswordHash("old-hash");
        member.setAuthVersion(4);
        ChangePasswordRequest request = new ChangePasswordRequest(
                "current-password", "new-password1", "new-password1");
        when(memberRepository.findById(1)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("current-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-password1", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-password1")).thenReturn("new-hash");
        when(memberRepository.changePasswordIfCurrent(1, "old-hash", 4, "new-hash")).thenReturn(1);

        memberService.changePassword(1, request);

        assertThat(member.getPasswordHash()).isEqualTo("old-hash");
        assertThat(member.getAuthVersion()).isEqualTo(4);
        verify(memberRepository).changePasswordIfCurrent(1, "old-hash", 4, "new-hash");
        verify(memberRepository, never()).saveAndFlush(member);
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        Member member = new Member();
        member.setMemberId(1);
        member.setPasswordHash("old-hash");
        ChangePasswordRequest request = new ChangePasswordRequest(
                "wrong-password", "new-password1", "new-password1");
        when(memberRepository.findById(1)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> memberService.changePassword(1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("目前密碼錯誤");
        verify(passwordEncoder, never()).encode(any());
        verify(memberRepository, never()).saveAndFlush(any());
    }

    @Test
    void changePasswordRejectsPasswordWithoutEnglishAndNumber() {
        Member member = new Member();
        member.setMemberId(1);
        member.setPasswordHash("old-hash");
        ChangePasswordRequest request = new ChangePasswordRequest(
                "current-password", "12345678", "12345678");
        when(memberRepository.findById(1)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("current-password", "old-hash")).thenReturn(true);

        assertThatThrownBy(() -> memberService.changePassword(1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("新密碼須包含英文與數字");

        verify(memberRepository, never()).changePasswordIfCurrent(any(), any(), anyInt(), any());
    }

    @Test
    void changePasswordRejectsWhenAccountChangedAfterCurrentPasswordCheck() {
        Member member = new Member();
        member.setMemberId(1);
        member.setPasswordHash("old-hash");
        member.setAuthVersion(4);
        ChangePasswordRequest request = new ChangePasswordRequest(
                "current-password", "new-password1", "new-password1");
        when(memberRepository.findById(1)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("current-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-password1", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-password1")).thenReturn("new-hash");
        when(memberRepository.changePasswordIfCurrent(1, "old-hash", 4, "new-hash")).thenReturn(0);

        assertThatThrownBy(() -> memberService.changePassword(1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("帳號狀態已變更，請重新登入後再試");
        verify(memberRepository, never()).saveAndFlush(any());
    }

    @Test
    void grantSellerRoleCreatesSellerMemberRole() {
        Member member = member(1);
        Role sellerRole = sellerRole();
        when(memberRepository.findById(1)).thenReturn(Optional.of(member));
        when(roleRepository.findByRoleName("seller")).thenReturn(Optional.of(sellerRole));
        when(memberRoleRepository.existsByMemberMemberIdAndRoleRoleId(1, 2)).thenReturn(false);

        memberService.grantSellerRole(1);

        ArgumentCaptor<MemberRole> captor = ArgumentCaptor.forClass(MemberRole.class);
        verify(memberRoleRepository).save(captor.capture());
        MemberRole savedMemberRole = captor.getValue();
        assertThat(savedMemberRole.getId()).isEqualTo(new MemberRoleId(1, 2));
        assertThat(savedMemberRole.getMember()).isSameAs(member);
        assertThat(savedMemberRole.getRole()).isSameAs(sellerRole);
    }

    @Test
    void grantSellerRoleDoesNotCreateDuplicateMemberRole() {
        Member member = member(1);
        Role sellerRole = sellerRole();
        when(memberRepository.findById(1)).thenReturn(Optional.of(member));
        when(roleRepository.findByRoleName("seller")).thenReturn(Optional.of(sellerRole));
        when(memberRoleRepository.existsByMemberMemberIdAndRoleRoleId(1, 2)).thenReturn(true);

        memberService.grantSellerRole(1);

        verify(memberRoleRepository, never()).save(any(MemberRole.class));
    }

    @Test
    void grantSellerRoleFailsWhenSellerRoleIsMissing() {
        Member member = member(1);
        when(memberRepository.findById(1)).thenReturn(Optional.of(member));
        when(roleRepository.findByRoleName("seller")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.grantSellerRole(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Role 'seller' is not configured");

        verify(memberRoleRepository, never()).save(any(MemberRole.class));
    }

    @Test
    void increaseAuthVersionInvalidatesExistingTokensAfterCommit() {
        when(memberRepository.increaseAuthVersion(1)).thenReturn(1);

        memberService.increaseAuthVersion(1);

        verify(memberRepository).increaseAuthVersion(1);
    }

    private Member member(Integer memberId) {
        Member member = new Member();
        member.setMemberId(memberId);
        return member;
    }

    private Role buyerRole() {
        Role role = new Role();
        role.setRoleId(1);
        role.setRoleName("buyer");
        return role;
    }

    private Role sellerRole() {
        Role role = new Role();
        role.setRoleId(2);
        role.setRoleName("seller");
        return role;
    }
}
