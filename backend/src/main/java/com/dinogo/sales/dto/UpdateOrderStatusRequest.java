package com.dinogo.sales.dto;

import com.dinogo.sales.entity.OrderStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateOrderStatusRequest(
        @NotNull
        OrderStatus status,

        @Size(max = 500)
        String reason) {
}
