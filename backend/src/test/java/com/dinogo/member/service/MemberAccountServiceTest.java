package com.dinogo.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.dinogo.member.dto.AdminMemberResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberAccountStatusHistoryRepository;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.seller.repository.SellerRepository;

@ExtendWith(MockitoExtension.class)
class MemberAccountServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberAccountStatusHistoryRepository historyRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberAccountService memberAccountService;

    @Test
    void suspendUsesConditionalStatusUpdateAndReturnsFreshState() {
        Member active = member(1, "ACTIVE");
        Member suspended = member(1, "SUSPENDED");
        when(memberRepository.findById(1)).thenReturn(Optional.of(active), Optional.of(suspended));
        when(memberRepository.updateStatusIfCurrent(1, "ACTIVE", "SUSPENDED")).thenReturn(1);

        AdminMemberResponse response = memberAccountService.suspend(1, 2, "違反規範");

        assertThat(response.status()).isEqualTo("SUSPENDED");
        verify(memberRepository).updateStatusIfCurrent(1, "ACTIVE", "SUSPENDED");
        verify(historyRepository).save(org.mockito.ArgumentMatchers.argThat(history ->
                "ACTIVE".equals(history.getPreviousStatus())
                        && "SUSPENDED".equals(history.getNewStatus())));
    }

    @Test
    void suspendRejectsWhenAccountStateChangedConcurrently() {
        Member active = member(1, "ACTIVE");
        when(memberRepository.findById(1)).thenReturn(Optional.of(active));
        when(memberRepository.updateStatusIfCurrent(1, "ACTIVE", "SUSPENDED")).thenReturn(0);

        assertThatThrownBy(() -> memberAccountService.suspend(1, 2, "違反規範"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("帳號狀態已變更，請重新操作");

        verify(historyRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private Member member(Integer memberId, String status) {
        Member member = new Member();
        member.setMemberId(memberId);
        member.setEmail("member@example.com");
        member.setLastName("王");
        member.setFirstName("小明");
        member.setStatus(status);
        return member;
    }
}
