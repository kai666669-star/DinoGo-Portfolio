package com.dinogo.review.dto.response;

import java.time.LocalDateTime;

/**
 * Keyset Pagination 的下一頁游標。
 *
 * <p>下一頁查詢必須同時帶入 reviewPriority、starUpdAt、starId 三個欄位。</p>
 */
public record ProductReviewCursor(
        Integer reviewPriority,
        LocalDateTime starUpdAt,
        Integer starId) {
}
