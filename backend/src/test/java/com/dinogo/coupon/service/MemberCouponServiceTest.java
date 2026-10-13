package com.dinogo.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dinogo.coupon.dto.MemberCouponResponse;
import com.dinogo.coupon.entity.Coupon;
import com.dinogo.coupon.entity.MemberCoupon;
import com.dinogo.coupon.repository.CouponRepository;
import com.dinogo.coupon.repository.MemberCouponRepository;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberCouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private MemberCouponRepository memberCouponRepository;

    @Mock
    private SellerRepository sellerRepository;

    private MemberCouponService memberCouponService;

    @BeforeEach
    void setUp() {
        memberCouponService = new MemberCouponService(
                couponRepository,
                memberCouponRepository,
                sellerRepository);
    }

    @Test
    void claimCouponSavesCouponForMember() {
        Coupon coupon = activeCoupon(100);
        when(sellerRepository.findById(3)).thenReturn(Optional.of(seller("店鋪 A")));
        when(couponRepository.findById(100)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByMemberIdAndCouponId(7, 100))
                .thenReturn(false);

        memberCouponService.claimCoupon(7, 100);

        verify(memberCouponRepository).save(any(MemberCoupon.class));
    }

    @Test
    void claimCouponRejectsDuplicateClaim() {
        Coupon coupon = activeCoupon(100);
        when(couponRepository.findById(100)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByMemberIdAndCouponId(7, 100))
                .thenReturn(true);

        assertThatThrownBy(() -> memberCouponService.claimCoupon(7, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("此會員已領取過這張優惠券");

        verify(memberCouponRepository, never()).save(any(MemberCoupon.class));
    }

    @Test
    void claimCouponRejectsWhenLimitIsReached() {
        Coupon coupon = activeCoupon(100);
        coupon.setLimitCount(2);
        when(couponRepository.findById(100)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByMemberIdAndCouponId(7, 100))
                .thenReturn(false);
        when(memberCouponRepository.countByCouponId(100)).thenReturn(2L);

        assertThatThrownBy(() -> memberCouponService.claimCoupon(7, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("此優惠券已達領取上限");

        verify(memberCouponRepository, never()).save(any(MemberCoupon.class));
    }

    @Test
    void claimCouponRejectsInactiveCoupon() {
        Coupon coupon = activeCoupon(100);
        coupon.setStatus("DISABLED");
        when(couponRepository.findById(100)).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> memberCouponService.claimCoupon(7, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("此優惠券已取消");

        verify(memberCouponRepository, never()).existsByMemberIdAndCouponId(any(), any());
    }

    @Test
    void getMemberCouponsMapsUsedCouponStatus() {
        Coupon coupon = activeCoupon(100);
        MemberCoupon memberCoupon = new MemberCoupon();
        memberCoupon.setMemberCouponId(10);
        memberCoupon.setMemberId(7);
        memberCoupon.setCouponId(100);
        memberCoupon.setUsed(true);
        memberCoupon.setUsedAt(LocalDateTime.now().minusHours(1));
        when(memberCouponRepository.findByMemberIdOrderByReceivedAtDesc(7))
                .thenReturn(List.of(memberCoupon));
        when(couponRepository.findById(100)).thenReturn(Optional.of(coupon));
        when(sellerRepository.findById(3)).thenReturn(Optional.of(seller("店鋪 A")));

        List<MemberCouponResponse> response = memberCouponService.getMemberCoupons(7);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().memberCouponId()).isEqualTo(10);
        assertThat(response.getFirst().sellerName()).isEqualTo("店鋪 A");
        assertThat(response.getFirst().status()).isEqualTo("USED");
    }

    @Test
    void getMemberCouponsKeepsRepeatCouponAvailableAfterUse() {
        Coupon coupon = activeCoupon(100);
        coupon.setPerMemberUsagePolicy("REPEAT");
        MemberCoupon memberCoupon = new MemberCoupon();
        memberCoupon.setMemberCouponId(10);
        memberCoupon.setMemberId(7);
        memberCoupon.setCouponId(100);
        memberCoupon.setUsed(true);
        memberCoupon.setUsedAt(LocalDateTime.now().minusHours(1));
        when(memberCouponRepository.findByMemberIdOrderByReceivedAtDesc(7))
                .thenReturn(List.of(memberCoupon));
        when(couponRepository.findById(100)).thenReturn(Optional.of(coupon));
        when(sellerRepository.findById(3)).thenReturn(Optional.of(seller("店鋪 A")));

        List<MemberCouponResponse> response = memberCouponService.getMemberCoupons(7);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().status()).isEqualTo("AVAILABLE");
    }

    @Test
    void getMemberCouponsMapsStartedDraftCouponAsAvailable() {
        Coupon coupon = activeCoupon(100);
        coupon.setStatus("DRAFT");
        MemberCoupon memberCoupon = new MemberCoupon();
        memberCoupon.setMemberCouponId(10);
        memberCoupon.setMemberId(7);
        memberCoupon.setCouponId(100);
        memberCoupon.setUsed(false);
        when(memberCouponRepository.findByMemberIdOrderByReceivedAtDesc(7))
                .thenReturn(List.of(memberCoupon));
        when(couponRepository.findById(100)).thenReturn(Optional.of(coupon));
        when(sellerRepository.findById(3)).thenReturn(Optional.of(seller("店鋪 A")));

        List<MemberCouponResponse> response = memberCouponService.getMemberCoupons(7);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().status()).isEqualTo("AVAILABLE");
    }

    private Coupon activeCoupon(Integer couponId) {
        Coupon coupon = new Coupon();
        coupon.setCouponId(couponId);
        coupon.setSellerId(3);
        coupon.setCouponCode("SAVE50");
        coupon.setCouponName("折扣券");
        coupon.setDiscountType("AMOUNT");
        coupon.setDiscountValue(new BigDecimal("50"));
        coupon.setScopeType("ALL");
        coupon.setStatus("ACTIVE");
        coupon.setStartAt(LocalDateTime.now().minusDays(1));
        coupon.setEndAt(LocalDateTime.now().plusDays(1));
        coupon.setUsedCount(0);
        coupon.setPerMemberUsagePolicy("ONCE");
        return coupon;
    }

    private Seller seller(String storeName) {
        Seller seller = new Seller();
        ReflectionTestUtils.setField(seller, "storeName", storeName);
        return seller;
    }
}
