package com.dinogo.review.dto.response;

import java.math.BigDecimal;

/**
 * 提供 product／seller 畫面顯示廠商評價；只統計已上架、已售出且有評分的商品。
 */
public record SellerRatingSummaryResponse(
        Integer sellerId,
        BigDecimal averageFiveStar,
        long ratingCount,
        long ratedProductCount) {
}
