package com.dinogo.sysmsg.exception;

/** 資源重複、冪等衝突或目前狀態造成的 HTTP 409。 */
public class SysmsgConflictException extends IllegalStateException {
    public SysmsgConflictException(String message) {
        super(message);
    }
}
