package com.dinogo.review.dto.response;

import java.util.List;

/**
 * 產品評論 Offset Pagination 回傳資料。
 *
 * @param content 實際回傳的評論，最多 10 筆
 * @param hasNext 是否還有下一頁
 * @param currentPage 一基底頁碼
 * @param totalElements 符合 productId 且已有星等的總筆數
 */
public record ProductReviewPageResponse(
        List<ProductReviewResponse> content,
        boolean hasNext,
        // 商品評價頁首使用，避免前端用已載入頁面推算錯誤總數。
        ProductReviewSummaryResponse summary,
        int currentPage,
        int totalPages,
        long totalElements) {
}
