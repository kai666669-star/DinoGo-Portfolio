package com.dinogo.coupon.dto;

import com.dinogo.coupon.entity.Coupon;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PublicCouponResponse(
        Integer couponId,
        String sellerName,
        String couponName,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minPurchaseAmount,
        LocalDateTime endAt,
        String scopeType,
        String perMemberUsagePolicy,
        String status
) {
    public static PublicCouponResponse from(Coupon coupon, String sellerName) {
        return new PublicCouponResponse(
                coupon.getCouponId(),
                sellerName,
                coupon.getCouponName(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinPurchaseAmount(),
                coupon.getEndAt(),
                coupon.getScopeType(),
                coupon.getPerMemberUsagePolicy(),
                coupon.getStatus()
        );
    }
}
