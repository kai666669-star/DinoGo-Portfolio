package com.dinogo.seller.dto;

import java.math.BigDecimal;

public record StorefrontSummaryResponse(
        BigDecimal averageRating,
        long ratingCount,
        long activeProductCount,
        long soldCount,
        long availableCouponCount) {
}
