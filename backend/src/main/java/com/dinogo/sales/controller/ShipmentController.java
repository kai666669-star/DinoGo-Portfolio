package com.dinogo.sales.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.sales.dto.shipment.CreateShipmentRequest;
import com.dinogo.sales.dto.shipment.ShipmentResponse;
import com.dinogo.sales.dto.shipment.ShipmentEventResponse;
import com.dinogo.sales.dto.shipment.SimulateTcatEventRequest;
import java.util.List;
import com.dinogo.sales.dto.shipment.UpdateShipmentStatusRequest;
import com.dinogo.sales.dto.shipment.UpdateShipmentTrackingInfoRequest;
import com.dinogo.sales.service.ShipmentService;
import com.dinogo.security.AuthenticatedMember;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders/{orderId}/shipment")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    public ResponseEntity<ShipmentResponse> createShipment(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody CreateShipmentRequest request) {
        ShipmentResponse response = shipmentService.createShipment(
                orderId, member.memberId(), request);
        return ResponseEntity
                .created(URI.create("/api/orders/" + orderId + "/shipment"))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<ShipmentResponse> getShipment(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal AuthenticatedMember member) {
        return ResponseEntity.ok(
                shipmentService.getShipment(orderId, member.memberId()));
    }

    @GetMapping("/events")
    public ResponseEntity<List<ShipmentEventResponse>> getShipmentEvents(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal AuthenticatedMember member) {
        return ResponseEntity.ok(shipmentService.getShipmentEvents(orderId, member.memberId()));
    }

    @PatchMapping("/status")
    public ResponseEntity<ShipmentResponse> updateShipmentStatus(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody UpdateShipmentStatusRequest request) {
        return ResponseEntity.ok(shipmentService.updateShipmentStatus(
                orderId, member.memberId(), request));
    }

    @PostMapping("/simulate-tcat-event")
    public ResponseEntity<ShipmentResponse> simulateTcatEvent(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody SimulateTcatEventRequest request) {
        return ResponseEntity.ok(shipmentService.simulateTcatEvent(orderId, member.memberId(), request));
    }

    @PatchMapping("/tracking-info")
    public ResponseEntity<ShipmentResponse> updateShipmentTrackingInfo(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody UpdateShipmentTrackingInfoRequest request) {
        return ResponseEntity.ok(shipmentService.updateShipmentTrackingInfo(
                orderId, member.memberId(), request));
    }

    @PatchMapping("/confirm-delivery")
    public ResponseEntity<ShipmentResponse> confirmDelivery(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal AuthenticatedMember member) {
        return ResponseEntity.ok(
                shipmentService.confirmDelivery(orderId, member.memberId()));
    }
}
