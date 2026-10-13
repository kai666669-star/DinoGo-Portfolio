package com.dinogo.review.dto.response;

//review-start，總共1次修改，第1次//
/** Review 模組自己的 Cloudinary 資產資料；前端顯示 secureUrl，後端保存 publicId。 */
public record ReviewImageAssetResponse(
        String assetId,
        String publicId,
        String secureUrl,
        String resourceType,
        String format,
        long bytes,
        Integer width,
        Integer height) {
}
//review-end，總共1次修改，第1次//

