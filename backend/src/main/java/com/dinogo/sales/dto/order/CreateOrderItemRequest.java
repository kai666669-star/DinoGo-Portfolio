package com.dinogo.sales.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 建立訂單時由前端提供的 SKU 與購買數量。 */
public record CreateOrderItemRequest(
        @NotNull(message = "SKU ID is required")
        @Positive(message = "SKU ID must be positive")
        Integer skuId,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        Integer quantity) {
}
