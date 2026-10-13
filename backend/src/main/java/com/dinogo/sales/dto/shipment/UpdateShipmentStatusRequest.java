package com.dinogo.sales.dto.shipment;

import com.dinogo.sales.entity.ShipmentStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateShipmentStatusRequest(
        @NotNull ShipmentStatus status) {
}
