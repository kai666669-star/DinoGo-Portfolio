package com.dinogo.sysmsg.dto.response;

import java.util.List;

//sysmsg-start，總共1次修改，第1次//
/** Cloudinary 訊息附件上傳結果；secureUrl 顯示，publicId 由後端持久化。 */
public record SysmsgImageUploadResponse(List<SysmsgImageAssetResponse> assets) {
}
//sysmsg-end，總共1次修改，第1次//

