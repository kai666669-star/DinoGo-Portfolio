package com.dinogo.review.repository;

import java.math.BigDecimal;

/** 功能：承接產品評價的單次 SQL 聚合結果；應用：避免頁面摘要拆成多次查詢。 */
public interface ProductReviewAggregate {
    BigDecimal getAverageFiveStar();
    long getTotalCount();
    long getFiveStarCount();
    long getFourStarCount();
    long getThreeStarCount();
    long getTwoStarCount();
    long getOneStarCount();
    long getWithFeedbackCount();
    long getWithImageCount();
}
