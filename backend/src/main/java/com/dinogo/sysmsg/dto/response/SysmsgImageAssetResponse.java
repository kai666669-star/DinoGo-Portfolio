package com.dinogo.sysmsg.dto.response;

//sysmsg-start，總共1次修改，第1次//
/** Sysmsg 模組自己的 Cloudinary 資產資料；前端顯示 secureUrl，後端保存 publicId。 */
public record SysmsgImageAssetResponse(
        String assetId,
        String publicId,
        String secureUrl,
        String resourceType,
        String format,
        long bytes,
        Integer width,
        Integer height) {
}
//sysmsg-end，總共1次修改，第1次//

