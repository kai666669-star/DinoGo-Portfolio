package com.dinogo.review.exception;

/**
 * 評論資源衝突。
 *
 * <p>例如兩個 COMPLETED 通知同時建立相同 orderId，或同一筆 Star 發生
 * 樂觀鎖衝突；ReviewExceptionHandler 會轉成 HTTP 409。</p>
 */
public class ReviewConflictException extends RuntimeException {

    public ReviewConflictException(String message) {
        super(message);
    }

    public ReviewConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
