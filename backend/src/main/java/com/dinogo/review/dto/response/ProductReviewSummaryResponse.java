package com.dinogo.review.dto.response;

import java.math.BigDecimal;

/**
 * 產品明細評價篩選列使用的完整彙總；數量皆以已有 fiveStar 的公開評價為母體。
 */
public record ProductReviewSummaryResponse(
        BigDecimal averageFiveStar,
        long totalCount,
        long fiveStarCount,
        long fourStarCount,
        long threeStarCount,
        long twoStarCount,
        long oneStarCount,
        long withFeedbackCount,
        long withImageCount) {
}
