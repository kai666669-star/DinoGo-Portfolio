package com.dinogo.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.dinogo.sales.dto.order.OrderItemResponse;
import com.dinogo.sales.entity.OrderStatus;

public record SellerOrderListResponse(
        Integer orderId,
        String orderNo,
        Integer buyerId,
        OrderStatus status,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        LocalDateTime createdAt,
        List<OrderItemResponse> items) {
}
