package com.dinogo.cart.dto;

import java.math.BigDecimal;

public record CheckoutPreviewResponse(
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal discount,
        BigDecimal totalAmount) {
}