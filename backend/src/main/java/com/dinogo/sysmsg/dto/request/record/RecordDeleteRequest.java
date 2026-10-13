package com.dinogo.sysmsg.dto.request.record;

/**
 * 收件匣刪除。
 *
 * record_id 建議從 PathVariable 取得：
 *
 * DELETE /api/sysmsg/records/{recordId}
 *
 * 因此 Request Body 不需要欄位。
 *
 * OA：
 *     硬刪除 Record
 *
 * OC / OS：
 *     硬刪除 Record
 *
 * 其他：
 *     RecordStatus → DELETE
 *
 * 不影響 SendEntity。
 */
public class RecordDeleteRequest {

    /*
     * 暫時不需要欄位。
     *
     * 保留此 DTO 是為了 API 架構一致。
     */

}
