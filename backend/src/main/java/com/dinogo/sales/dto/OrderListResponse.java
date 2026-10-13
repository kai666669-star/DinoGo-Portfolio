package com.dinogo.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.dinogo.sales.dto.order.OrderItemResponse;
import com.dinogo.sales.dto.order.OrderShipmentSummary;
import com.dinogo.sales.entity.OrderStatus;

public record OrderListResponse(Integer orderId,
        String orderNo,
        Integer sellerId,
        OrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        List<OrderItemResponse> items,
        OrderShipmentSummary shipment) {
}
