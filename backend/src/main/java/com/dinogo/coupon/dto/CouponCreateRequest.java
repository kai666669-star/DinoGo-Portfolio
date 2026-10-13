package com.dinogo.coupon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponCreateRequest(
        @NotBlank String couponCode,
        @NotBlank String couponName,
        @NotBlank String discountType,
        @NotNull @DecimalMin("0.01") BigDecimal discountValue,
        BigDecimal minPurchaseAmount,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        Integer limitCount,
        String perMemberUsagePolicy,
        @NotBlank String scopeType,
        Integer categoryId,
        Integer productId
) {
}
