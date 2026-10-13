package com.dinogo.sales.dto.shipment;

import com.dinogo.sales.entity.ShipmentEventType;
import jakarta.validation.constraints.NotNull;

public record SimulateTcatEventRequest(@NotNull ShipmentEventType eventType) {}
