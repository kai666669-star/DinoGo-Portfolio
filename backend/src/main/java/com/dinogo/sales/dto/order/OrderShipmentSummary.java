package com.dinogo.sales.dto.order;

import java.time.LocalDateTime;

import com.dinogo.sales.entity.ShipmentStatus;

public record OrderShipmentSummary(
        Integer shipmentId,
        String carrierName,
        String trackingNo,
        ShipmentStatus status,
        LocalDateTime shippedAt,
        LocalDateTime availablePickupAt,
        LocalDateTime deliveredAt) {
}
