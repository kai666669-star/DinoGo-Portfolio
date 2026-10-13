package com.dinogo.seller.service;

//rev+msg-start，總共1次修改，第1次//
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dinogo.member.dto.MemberSysmsgResponse;
import com.dinogo.member.entity.Member;
import com.dinogo.member.service.MemberSysmsgProviderService;
import com.dinogo.seller.dto.SellerSysmsgResponse;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

@ExtendWith(MockitoExtension.class)
class SellerSysmsgProviderServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private MemberSysmsgProviderService memberProvider;

    private SellerSysmsgProviderService service;

    @BeforeEach
    void setUp() {
        service = new SellerSysmsgProviderService(sellerRepository, memberProvider);
    }

    @Test
    void getSellerReturnsActiveSellerAndMemberNotificationData() {
        Seller seller = seller(9, 8, "恐龍商店", " seller@example.com ", true, false);
        when(sellerRepository.findBySellerIdAndStatusIgnoreCase(9, "ACTIVE"))
                .thenReturn(Optional.of(seller));

        SellerSysmsgResponse response = service.getSeller(9);

        assertThat(response.sellerId()).isEqualTo(9);
        assertThat(response.memberId()).isEqualTo(8);
        assertThat(response.active()).isTrue();
        assertThat(response.sellerName()).isEqualTo("恐龍商店");
        assertThat(response.email()).isEqualTo("seller@example.com");
        assertThat(response.emailOrderNotifications()).isTrue();
        assertThat(response.emailMarketingNotifications()).isFalse();
        verify(memberProvider, never()).getMember(8);
    }

    @Test
    void getSellerRejectsMissingOrInactiveSeller() {
        when(sellerRepository.findBySellerIdAndStatusIgnoreCase(9, "ACTIVE"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSeller(9))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Active seller not found: 9");
    }

    @Test
    void getSellerFallsBackToMemberProviderWhenSellerEmailIsBlank() {
        Seller seller = seller(9, 8, "恐龍商店", " ", true, false);
        when(sellerRepository.findBySellerIdAndStatusIgnoreCase(9, "ACTIVE"))
                .thenReturn(Optional.of(seller));
        when(memberProvider.getMember(8)).thenReturn(new MemberSysmsgResponse(
                8, 9, false, "fallback@example.com", "seller", List.of(1, 2), false, true));

        SellerSysmsgResponse response = service.getSeller(9);

        assertThat(response.email()).isEqualTo("fallback@example.com");
        assertThat(response.emailOrderNotifications()).isFalse();
        assertThat(response.emailMarketingNotifications()).isTrue();
    }

    @Test
    void getAllSellersUsesOnlyActiveSellerQuery() {
        Seller seller = seller(9, 8, "恐龍商店", "seller@example.com", true, true);
        when(sellerRepository.findAllByStatusIgnoreCase("ACTIVE")).thenReturn(List.of(seller));

        List<SellerSysmsgResponse> responses = service.getAllSellers();

        assertThat(responses).extracting(SellerSysmsgResponse::sellerId).containsExactly(9);
        assertThat(responses).allMatch(SellerSysmsgResponse::active);
    }

    private Seller seller(
            Integer sellerId,
            Integer memberId,
            String storeName,
            String email,
            boolean orderNotifications,
            boolean marketingNotifications) {
        Member member = new Member();
        member.setMemberId(memberId);
        member.setEmail(email);
        member.setEmailOrderNotifications(orderNotifications);
        member.setEmailMarketingNotifications(marketingNotifications);

        Seller seller = new Seller();
        seller.setSellerId(sellerId);
        seller.setMember(member);
        seller.setStoreName(storeName);
        seller.setStatus("ACTIVE");
        return seller;
    }
}
//rev+msg-end，總共1次修改，第1次//
