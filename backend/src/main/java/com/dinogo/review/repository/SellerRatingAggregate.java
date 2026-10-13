package com.dinogo.review.repository;

import java.math.BigDecimal;

/** 功能：承接商家評價的單次 SQL 聚合結果；應用：一次取得平均、評分數及商品數。 */
public interface SellerRatingAggregate {
    BigDecimal getAverageFiveStar();
    long getRatingCount();
    long getRatedProductCount();
}
