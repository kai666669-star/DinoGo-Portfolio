package com.dinogo.sales.dto.shipment;

import java.time.LocalDateTime;

import com.dinogo.sales.entity.ShipmentStatus;

public record ShipmentResponse(
        Integer shipmentId,
        Integer orderId,
        String carrierName,
        String trackingNo,
        ShipmentStatus status,
        LocalDateTime shippedAt,
        LocalDateTime availablePickupAt,
        LocalDateTime deliveredAt,
        String deliveryPhotoUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
