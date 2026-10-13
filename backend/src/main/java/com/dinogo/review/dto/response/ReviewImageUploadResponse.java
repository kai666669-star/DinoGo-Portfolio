package com.dinogo.review.dto.response;

import java.util.List;

//review-start，總共1次修改，第1次//
/** Cloudinary 評論圖片上傳結果；secureUrl 顯示，publicId 由後端持久化。 */
public record ReviewImageUploadResponse(List<ReviewImageAssetResponse> assets) {
}
//review-end，總共1次修改，第1次//

