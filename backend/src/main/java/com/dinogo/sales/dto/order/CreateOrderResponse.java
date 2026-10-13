package com.dinogo.sales.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.dinogo.sales.entity.OrderStatus;

/** 建立訂單成功後回傳的訂單識別與金額資訊。 */
public record CreateOrderResponse(
        Integer orderId,
        String orderNo,
        OrderStatus status,
        BigDecimal subtotalAmount,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        LocalDateTime createdAt) {
}
