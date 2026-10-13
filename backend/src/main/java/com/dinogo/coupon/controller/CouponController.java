package com.dinogo.coupon.controller;

import com.dinogo.coupon.dto.CouponCreateRequest;
import com.dinogo.coupon.dto.CouponResponse;
import com.dinogo.coupon.dto.CouponUpdateRequest;
import com.dinogo.coupon.service.CouponService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.dinogo.security.AuthenticatedMember;
import com.dinogo.seller.service.CurrentSellerService;

@RestController
@RequestMapping("/api/seller/coupons")
public class CouponController {

    private final CouponService couponService;
    private final CurrentSellerService currentSellerService;

    public CouponController(CouponService couponService, CurrentSellerService currentSellerService) {
        this.couponService = couponService;
        this.currentSellerService = currentSellerService;
    }

    @GetMapping
    public List<CouponResponse> getCoupons(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam(required = false) Integer sellerId) {
        return couponService.getCoupons(currentSellerId(member, sellerId));
    }

    @GetMapping("/{couponId}")
    public CouponResponse getCoupon(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam(required = false) Integer sellerId,
            @PathVariable Integer couponId) {
        return couponService.getCoupon(currentSellerId(member, sellerId), couponId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse createCoupon(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam(required = false) Integer sellerId,
            @Valid @RequestBody CouponCreateRequest request
    ) {
        return couponService.createCoupon(currentSellerId(member, sellerId), request);
    }

    @PutMapping("/{couponId}")
    public CouponResponse updateCoupon(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam(required = false) Integer sellerId,
            @PathVariable Integer couponId,
            @Valid @RequestBody CouponUpdateRequest request
    ) {
        return couponService.updateCoupon(currentSellerId(member, sellerId), couponId, request);
    }

    @PatchMapping("/{couponId}/activate")
    public CouponResponse activateCoupon(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam(required = false) Integer sellerId,
            @PathVariable Integer couponId) {
        return couponService.activateCoupon(currentSellerId(member, sellerId), couponId);
    }

    @PatchMapping("/{couponId}/disable")
    public CouponResponse disableCoupon(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam(required = false) Integer sellerId,
            @PathVariable Integer couponId) {
        return couponService.disableCoupon(currentSellerId(member, sellerId), couponId);
    }

    private Integer currentSellerId(AuthenticatedMember member, Integer requestedSellerId) {
        return currentSellerService.requireMatchingActiveSellerId(member.memberId(), requestedSellerId);
    }
}
