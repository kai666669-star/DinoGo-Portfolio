package com.dinogo.sysmsg.service;

/**
 * Email 建立階段的不可重試錯誤。
 * 功能：以穩定 failureCode 區分地址與郵件內容問題。
 * 應用：Dispatcher 會將這類錯誤直接送入死信，避免無意義重送。
 */
public class EmailDeliveryPreparationException extends IllegalStateException {
    private final String failureCode;

    public EmailDeliveryPreparationException(String failureCode, String message) {
        super(message);
        this.failureCode = failureCode;
    }

    public String getFailureCode() {
        return failureCode;
    }
}
