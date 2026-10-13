package com.dinogo.coupon.dto;

import com.dinogo.coupon.entity.Coupon;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(
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
        Integer limitCount,
        Integer usedCount,
        String perMemberUsagePolicy,
        String scopeType,
        Integer categoryId,
        Integer productId,
        String status
) {
    public static CouponResponse from(Coupon coupon, String sellerName) {
        return new CouponResponse(
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
                coupon.getLimitCount(),
                coupon.getUsedCount(),
                coupon.getPerMemberUsagePolicy(),
                coupon.getScopeType(),
                coupon.getCategoryId(),
                coupon.getProductId(),
                coupon.getStatus()
        );
    }
}
