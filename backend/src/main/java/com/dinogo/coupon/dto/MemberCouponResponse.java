package com.dinogo.coupon.dto;

import com.dinogo.coupon.entity.Coupon;
import com.dinogo.coupon.entity.MemberCoupon;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MemberCouponResponse(
        Integer memberCouponId,
        Integer couponId,
        Integer sellerId,
        String sellerName,
        String couponCode,
        String couponName,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minPurchaseAmount,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String scopeType,
        Integer categoryId,
        Integer productId,
        String perMemberUsagePolicy,
        LocalDateTime receivedAt,
        LocalDateTime usedAt,
        String status) {
    public static MemberCouponResponse from(
            MemberCoupon memberCoupon,
            Coupon coupon,
            LocalDateTime now,
            String sellerName) {
        String status;

        if (Boolean.TRUE.equals(memberCoupon.getUsed())
                && !"REPEAT".equals(coupon.getPerMemberUsagePolicy())) {
            status = "USED";
        } else if (coupon.getEndAt().isBefore(now)) {
            status = "EXPIRED";
        } else if ("DISABLED".equals(coupon.getStatus())) {
            status = "DISABLED";
        } else if (coupon.getStartAt().isAfter(now)) {
            status = "NOT_STARTED";
        } else if ("ACTIVE".equals(coupon.getStatus())
                || "DRAFT".equals(coupon.getStatus())) {
            status = "AVAILABLE";
        } else {
            status = "NOT_AVAILABLE";
        }

        return new MemberCouponResponse(
                memberCoupon.getMemberCouponId(),
                coupon.getCouponId(),
                coupon.getSellerId(),
                sellerName,
                coupon.getCouponCode(),
                coupon.getCouponName(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinPurchaseAmount(),
                coupon.getStartAt(),
                coupon.getEndAt(),
                coupon.getScopeType(),
                coupon.getCategoryId(),
                coupon.getProductId(),
                coupon.getPerMemberUsagePolicy(),
                memberCoupon.getReceivedAt(),
                memberCoupon.getUsedAt(),
                status);
    }
}
