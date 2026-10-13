package com.dinogo.review.dto.response;

import java.math.BigDecimal;

/** 提供同一單體內其他模組讀取商品評價統計，不暴露 Review Entity。 */
public record ProductRatingSummaryResponse(
        Integer productId,
        BigDecimal averageFiveStar,
        long ratingCount) {
}
