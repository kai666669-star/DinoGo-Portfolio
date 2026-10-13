package com.dinogo.sales.dto.payment;

import jakarta.validation.constraints.NotBlank;

public record CreatePaymentRequest(

        @NotBlank(message = "Payment method is required")
        String paymentMethodCode) {
}