package com.dinogo.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.dinogo.sales.dto.order.OrderItemResponse;
import com.dinogo.sales.dto.order.OrderPaymentSummary;
import com.dinogo.sales.dto.order.OrderShipmentSummary;
import com.dinogo.sales.entity.OrderStatus;

public record OrderDetailResponse(Integer orderId,
        String orderNo,
        Integer buyerId,
        Integer sellerId,
        Integer addressId,
        String receiverName,
        String receiverPhone,
        String shippingPostalCode,
        String shippingCity,
        String shippingDistrict,
        String shippingDetailAddress,
        OrderStatus status,
        BigDecimal subtotalAmount,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String buyerRemark,
        String cancelReason,
        String cancelledBy,
        LocalDateTime cancelledAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items,
        OrderPaymentSummary payment,
        OrderShipmentSummary shipment) {
}
