package com.dinogo.sales.dto.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 建立訂單請求；價格、賣家及收件資料快照均由後端重新取得。 */
public record CreateOrderRequest(
        @NotNull(message = "Address ID is required")
        @Positive(message = "Address ID must be positive")
        Integer addressId,

        @NotBlank(message = "Shipping method is required")
        String shippingMethod,

        @Size(max = 500, message = "Buyer remark must not exceed 500 characters")
        String buyerRemark,

        @Positive(message = "Member coupon ID must be positive")
        Integer memberCouponId,

        @NotEmpty(message = "Order items are required")
        List<@Valid CreateOrderItemRequest> items) {
}
