package com.dinogo.review.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.dinogo.review.entity.StarEntity;

/**
 * 訂單商品評論回傳 DTO，不直接把 StarEntity 傳給 Vue。
 *
 * @param reviewed fiveStar 大於 0 時為 true；完成訂單商品明細用此變數切換按鈕樣式
 * @param reviewPriority SQL Server computed column 的唯讀結果
 * @param version JPA 樂觀鎖版本
 * @param starUpdAt INSERT 由 SQL Server 建立，UPDATE 由 Spring Boot 更新
 */
public record StarResponse(
        Integer starId,
        Integer orderItemId,
        Integer productId,
        String productName,
        String imageUrl,
        BigDecimal basePrice,
        //review-start，總共1次修改，第1次//
        String imgOne,
        String imgOnePublicId,
        String imgTwo,
        String imgTwoPublicId,
        String imgThree,
        String imgThreePublicId,
        //review-end，總共1次修改，第1次//
        String feedback,
        Integer fiveStar,
        boolean reviewed,
        Integer reviewPriority,
        Long version,
        LocalDateTime starUpdAt) {

    /** Entity 轉 DTO；reviewed 明確由 fiveStar 是否大於 0 即時計算。 */
    public static StarResponse fromEntity(StarEntity star) {
        return new StarResponse(
                star.getId(),
                star.getOrderItemId(),
                star.getProductId(),
                star.getProductName(),
                star.getImageUrl(),
                star.getBasePrice(),
                star.getImgOne(),
                star.getImgOnePublicId(),
                star.getImgTwo(),
                star.getImgTwoPublicId(),
                star.getImgThree(),
                star.getImgThreePublicId(),
                star.getFeedback(),
                star.getFiveStar(),
                star.getFiveStar() != null && star.getFiveStar() > 0,
                star.getReviewPriority(),
                star.getVersion(),
                star.getStarUpdAt());
    }
}

