package com.dinogo.cart.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CheckoutPreviewRequest(

        @NotEmpty(message = "結帳商品不可為空") List<@Valid CheckoutPreviewItemRequest> items,

        @NotNull(message = "Address ID 不可為空") @Positive(message = "Address ID 必須大於0") Integer addressId,

        @NotNull(message = "Shipping method 不可為空") String shippingMethod,

        @NotNull(message = "Payment method 不可為空") String paymentMethod,

        // 會員領取的優惠券 ID
        Integer memberCouponId) {
}