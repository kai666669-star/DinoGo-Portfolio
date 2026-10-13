package com.dinogo.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 修改單筆評論的 Request DTO。
 *
 * <p>memberId 不放在 Request Body；會員身分由 Authorization 對應的登入模組取得，
 * 不信任前端自行傳入的 memberId。圖片先上傳 Cloudinary，此 DTO 只接收 HTTPS URL。</p>
 */
//review-start，總共1次修改，第1次//
public record StarUpdateRequest(
        @NotNull(message = "fiveStar 不可為空")
        @Min(value = 1, message = "fiveStar 最小為 1")
        @Max(value = 5, message = "fiveStar 最大為 5")
        Integer fiveStar,

        @Size(max = 500, message = "feedback 最多 500 字")
        String feedback,

        @Size(max = 500, message = "imgOne URL 最多 500 字") String imgOne,
        @Size(max = 255, message = "imgOnePublicId 最多 255 字") String imgOnePublicId,
        @Size(max = 500, message = "imgTwo URL 最多 500 字") String imgTwo,
        @Size(max = 255, message = "imgTwoPublicId 最多 255 字") String imgTwoPublicId,
        @Size(max = 500, message = "imgThree URL 最多 500 字") String imgThree,
        @Size(max = 255, message = "imgThreePublicId 最多 255 字") String imgThreePublicId) {
}
//review-end，總共1次修改，第1次//

