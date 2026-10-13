package com.dinogo.review.dto.response;

import java.time.LocalDateTime;

import com.dinogo.review.entity.StarEntity;

/**
 * 產品介紹頁使用的公開評論 DTO。
 *
 * <p>公開回應不包含原始 memberId；匿名名稱由後端直接提供。</p>
 */
public record ProductReviewResponse(
        Integer starId,
        //review-start，總共3次修改，第1次//
        String reviewerDisplayName,
        //review-end，總共3次修改，第1次//
        Integer productId,
        String productName,
        Integer fiveStar,
        String feedback,
        //review-start，總共3次修改，第2次//
        String imgOne,
        String imgTwo,
        String imgThree,
        //review-end，總共3次修改，第2次//
        LocalDateTime starUpdAt) {

    public static ProductReviewResponse fromEntity(StarEntity star) {
        return new ProductReviewResponse(
                star.getId(),
                //review-start，總共3次修改，第3次//
                maskMemberId(star.getHistory().getMemberId()),
                //review-end，總共3次修改，第3次//
                star.getProductId(),
                star.getProductName(),
                star.getFiveStar(),
                star.getFeedback(),
                star.getImgOne(),
                star.getImgTwo(),
                star.getImgThree(),
                star.getStarUpdAt());
    }

    /** 公開頁面只提供部分遮罩後的會員編號，不暴露完整 memberId。 */
    private static String maskMemberId(Integer memberId) {
        String value = memberId == null ? "" : memberId.toString();
        if (value.isEmpty()) {
            return "會員 *****";
        }
        if (value.length() == 1) {
            return "會員 " + value + "*****";
        }
        return "會員 " + value.charAt(0) + "*****" + value.charAt(value.length() - 1);
    }
}

