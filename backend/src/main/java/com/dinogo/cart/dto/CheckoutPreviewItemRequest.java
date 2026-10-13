package com.dinogo.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CheckoutPreviewItemRequest(

        @NotNull(message = "SKU ID 不可為空") @Positive(message = "SKU ID 必須大於 0") Integer skuId,

        @NotNull(message = "商品數量不可為空") @Positive(message = "商品數量必須大於 0") Integer quantity) {
}