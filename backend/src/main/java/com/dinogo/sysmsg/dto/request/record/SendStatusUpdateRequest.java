package com.dinogo.sysmsg.dto.request.record;

import com.dinogo.sysmsg.entity.SendStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Send 狀態修改。
 *
 * 合法：
 *
 * SAVE → SEND
 * SAVE → DELETE
 * SEND → DELETE
 *
 * DELETE → X
 *
 * 實際是否合法由 SendService.changeSendStatus()
 * 判斷。
 *
 * DTO 只負責接收目標狀態。
 */
public class SendStatusUpdateRequest {

    @NotNull
    private SendStatus targetStatus;

    public SendStatusUpdateRequest() {
    }

    public SendStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(SendStatus targetStatus) {
        this.targetStatus = targetStatus;
    }
}
