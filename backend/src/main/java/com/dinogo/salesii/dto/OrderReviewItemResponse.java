package com.dinogo.salesii.dto;

import java.math.BigDecimal;

//review-start，總共1次修改，第1次//
/**
 * 功能：提供完成訂單中的商品快照給 Review。
 * 應用：Review 不直接依賴 sales OrderItem Entity，避免跨模組持久化物件耦合。
 */
public record OrderReviewItemResponse(
        Integer orderItemId,
        Integer productId,
        String productName,
        String productImageUrl,
        BigDecimal unitPrice) {
}
//review-end，總共1次修改，第1次//
