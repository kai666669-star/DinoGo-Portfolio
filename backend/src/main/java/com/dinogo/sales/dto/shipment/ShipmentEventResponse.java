package com.dinogo.sales.dto.shipment;

import java.time.LocalDateTime;
import com.dinogo.sales.entity.ShipmentEventSource;
import com.dinogo.sales.entity.ShipmentEventType;

public record ShipmentEventResponse(Integer shipmentEventId, ShipmentEventType eventType,
        ShipmentEventSource source, String remark, LocalDateTime occurredAt) {}
