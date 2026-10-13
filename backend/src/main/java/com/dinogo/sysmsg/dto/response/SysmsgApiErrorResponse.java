package com.dinogo.sysmsg.dto.response;

import java.time.LocalDateTime;

/** 與模擬後端 OrderApiErrorResponse 對齊的 sysmsg 統一錯誤格式。 */
public record SysmsgApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path) {
}
