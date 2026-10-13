package com.dinogo.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.dinogo.member.dto.MemberSysmsgResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.entity.MemberRole;
import com.dinogo.member.entity.Role;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

@ExtendWith(MockitoExtension.class)
class MemberSysmsgProviderServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SellerRepository sellerRepository;

    @InjectMocks
    private MemberSysmsgProviderService providerService;

    @Test
    void getProfileReturnsActiveMemberDataForAuthenticatedCaller() {
        Member member = activeMember(1, "buyer@example.com", 1, "buyer");
        member.setEmailOrderNotifications(false);
        member.setEmailMarketingNotifications(true);
        Seller seller = new Seller();
        seller.setSellerId(8);
        when(memberRepository.findById(1)).thenReturn(Optional.of(member));
        when(sellerRepository.findByMember_MemberId(1)).thenReturn(Optional.of(seller));

        MemberSysmsgResponse response = providerService.getProfile(1);

        assertThat(response.memberId()).isEqualTo(1);
        assertThat(response.sellerId()).isEqualTo(8);
        assertThat(response.authenticated()).isTrue();
        assertThat(response.email()).isEqualTo("buyer@example.com");
        assertThat(response.role()).isEqualTo("buyer");
        assertThat(response.roleIds()).containsExactly(1);
        assertThat(response.emailOrderNotifications()).isFalse();
        assertThat(response.emailMarketingNotifications()).isTrue();
    }

    @Test
    void getMemberRejectsInactiveMember() {
        Member member = activeMember(2, "inactive@example.com", 1, "buyer");
        member.setStatus("INACTIVE");
        when(memberRepository.findById(2)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> providerService.getMember(2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Member not found: 2");
    }

    @Test
    void getAllMembersReturnsOnlyActiveMembers() {
        Member activeMember = activeMember(1, "active@example.com", 1, "buyer");
        Member inactiveMember = activeMember(2, "inactive@example.com", 1, "buyer");
        inactiveMember.setStatus("SUSPENDED");
        when(memberRepository.findAllByStatusIgnoreCase("ACTIVE")).thenReturn(List.of(activeMember));
        when(sellerRepository.findByMember_MemberIdIn(List.of(1))).thenReturn(List.of());

        List<MemberSysmsgResponse> responses = providerService.getAllMembers();

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.memberId()).isEqualTo(1);
            assertThat(response.authenticated()).isFalse();
        });
        verify(sellerRepository, never()).findByMember_MemberId(1);
    }

    @Test
    void getMemberAddsBuyerRoleForSeller() {
        Member member = activeMember(3, "seller@example.com", 2, "seller");
        when(memberRepository.findById(3)).thenReturn(Optional.of(member));
        when(sellerRepository.findByMember_MemberId(3)).thenReturn(Optional.empty());

        MemberSysmsgResponse response = providerService.getMember(3);

        assertThat(response.roleIds()).containsExactly(1, 2);
    }

    @Test
    void getMemberReturnsAdminRoleId() {
        Member member = activeMember(4, "admin@example.com", 3, "admin");
        when(memberRepository.findById(4)).thenReturn(Optional.of(member));
        when(sellerRepository.findByMember_MemberId(4)).thenReturn(Optional.empty());

        MemberSysmsgResponse response = providerService.getMember(4);

        assertThat(response.roleIds()).containsExactly(3);
    }

    private Member activeMember(Integer memberId, String email, Integer roleId, String roleName) {
        Role role = new Role();
        role.setRoleId(roleId);
        role.setRoleName(roleName);
        MemberRole memberRole = new MemberRole();
        memberRole.setRole(role);

        Member member = new Member();
        member.setMemberId(memberId);
        member.setEmail(email);
        member.setStatus("ACTIVE");
        member.setMemberRoles(List.of(memberRole));
        return member;
    }
}
