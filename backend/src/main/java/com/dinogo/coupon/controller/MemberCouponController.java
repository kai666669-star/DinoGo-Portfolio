package com.dinogo.coupon.controller;

import com.dinogo.coupon.dto.CouponResponse;
import com.dinogo.coupon.dto.MemberCouponResponse;
import com.dinogo.coupon.service.MemberCouponService;
import com.dinogo.security.AuthenticatedMember;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member/coupons")
public class MemberCouponController {

    private final MemberCouponService memberCouponService;

    public MemberCouponController(
            MemberCouponService memberCouponService) {
        this.memberCouponService = memberCouponService;
    }

    @GetMapping
    public List<MemberCouponResponse> getMemberCoupons(
            @AuthenticationPrincipal AuthenticatedMember member) {
        return memberCouponService.getMemberCoupons(
                member.memberId());
    }

    @PostMapping("/{couponId}/claim")
    public CouponResponse claimCoupon(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Integer couponId) {
        return memberCouponService.claimCoupon(
                member.memberId(),
                couponId);
    }
}