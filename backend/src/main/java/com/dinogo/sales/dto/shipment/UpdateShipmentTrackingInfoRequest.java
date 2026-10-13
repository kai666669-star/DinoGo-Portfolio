package com.dinogo.sales.dto.shipment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateShipmentTrackingInfoRequest(
        @NotBlank(message = "Carrier name is required")
        @Size(max = 100, message = "Carrier name must not exceed 100 characters")
        String carrierName,
        @NotBlank(message = "Tracking number is required")
        @Size(max = 100, message = "Tracking number must not exceed 100 characters")
        String trackingNo) {
}
