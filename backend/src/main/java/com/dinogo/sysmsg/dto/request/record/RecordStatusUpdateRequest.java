package com.dinogo.sysmsg.dto.request.record;

import com.dinogo.sysmsg.entity.RecordStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 閱讀訊息。
 *
 * 目前主要：
 *
 * UNREAD → READ
 *
 * 不允許由此 DTO 任意修改 DELETE。
 */
public class RecordStatusUpdateRequest {

    //msg-已讀回傳後端之資訊// 前端固定送出 targetStatus=READ，不接受 DELETE 或任意狀態。
    @NotNull
    private RecordStatus targetStatus;

    public RecordStatusUpdateRequest() {
    }

    public RecordStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(RecordStatus targetStatus) {
        this.targetStatus = targetStatus;
    }
}
