package com.dinogo.sales.dto.order;

import java.time.LocalDateTime;

import com.dinogo.sales.entity.PaymentStatus;

public record OrderPaymentSummary(
        Integer paymentId,
        String paymentMethodCode,
        String paymentMethodName,
        PaymentStatus status,
        String failureReason,
        LocalDateTime paidAt,
        LocalDateTime createdAt) {
}
