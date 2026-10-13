package com.dinogo.sales.dto.payment;

import com.dinogo.sales.entity.PaymentStatus;

import jakarta.validation.constraints.NotNull;

public record SimulatePaymentRequest(
        @NotNull(message = "Payment result is required")
        PaymentStatus status,
        String failureReason) {
}
