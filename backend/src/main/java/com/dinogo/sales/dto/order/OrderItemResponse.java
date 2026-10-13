package com.dinogo.sales.dto.order;

import java.math.BigDecimal;

public record OrderItemResponse( Integer orderItemId,
        Integer productId,
        Integer skuId,
        String productName,
        String skuSpec,
        String productImageUrl,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        Boolean isReviewed) {
}
