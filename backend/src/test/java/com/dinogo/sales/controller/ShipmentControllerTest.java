package com.dinogo.sales.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.dinogo.sales.dto.shipment.CreateShipmentRequest;
import com.dinogo.sales.dto.shipment.ShipmentResponse;
import com.dinogo.sales.dto.shipment.ShipmentEventResponse;
import com.dinogo.sales.dto.shipment.SimulateTcatEventRequest;
import java.util.List;
import com.dinogo.sales.dto.shipment.UpdateShipmentStatusRequest;
import com.dinogo.sales.dto.shipment.UpdateShipmentTrackingInfoRequest;
import com.dinogo.sales.entity.ShipmentEventType;
import com.dinogo.sales.entity.ShipmentStatus;
import com.dinogo.sales.service.ShipmentService;
import com.dinogo.security.AuthenticatedMember;

class ShipmentControllerTest {

    @Test
    void createShipmentUsesAuthenticatedMemberAndReturnsLocation() {
        ShipmentService shipmentService = mock(ShipmentService.class);
        AuthenticatedMember member = new AuthenticatedMember(8, "seller@example.com");
        CreateShipmentRequest request = new CreateShipmentRequest("Black Cat", "TRACK-1");
        ShipmentResponse serviceResponse = response();
        when(shipmentService.createShipment(10, 8, request)).thenReturn(serviceResponse);

        var actual = new ShipmentController(shipmentService)
                .createShipment(10, member, request);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(actual.getHeaders().getLocation())
                .hasToString("/api/orders/10/shipment");
        assertThat(actual.getBody()).isSameAs(serviceResponse);
        verify(shipmentService).createShipment(10, 8, request);
    }

    @Test
    void getShipmentUsesAuthenticatedMember() {
        ShipmentService shipmentService = mock(ShipmentService.class);
        AuthenticatedMember member = new AuthenticatedMember(6, "buyer@example.com");
        ShipmentResponse serviceResponse = response();
        when(shipmentService.getShipment(10, 6)).thenReturn(serviceResponse);

        var actual = new ShipmentController(shipmentService)
                .getShipment(10, member);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(serviceResponse);
        verify(shipmentService).getShipment(10, 6);
    }

    @Test
    void getShipmentEventsUsesAuthenticatedMember() {
        ShipmentService shipmentService = mock(ShipmentService.class);
        AuthenticatedMember member = new AuthenticatedMember(6, "buyer@example.com");
        List<ShipmentEventResponse> events = List.of();
        when(shipmentService.getShipmentEvents(10, 6)).thenReturn(events);

        var actual = new ShipmentController(shipmentService).getShipmentEvents(10, member);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(events);
        verify(shipmentService).getShipmentEvents(10, 6);
    }

    @Test
    void simulateTcatEventUsesAuthenticatedSeller() {
        ShipmentService shipmentService = mock(ShipmentService.class);
        AuthenticatedMember member = new AuthenticatedMember(8, "seller@example.com");
        SimulateTcatEventRequest request = new SimulateTcatEventRequest(ShipmentEventType.IN_TRANSIT);
        ShipmentResponse serviceResponse = response();
        when(shipmentService.simulateTcatEvent(10, 8, request)).thenReturn(serviceResponse);

        var actual = new ShipmentController(shipmentService)
                .simulateTcatEvent(10, member, request);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(serviceResponse);
        verify(shipmentService).simulateTcatEvent(10, 8, request);
    }

    @Test
    void updateShipmentStatusUsesAuthenticatedMember() {
        ShipmentService shipmentService = mock(ShipmentService.class);
        AuthenticatedMember member = new AuthenticatedMember(8, "seller@example.com");
        UpdateShipmentStatusRequest request = new UpdateShipmentStatusRequest(
                ShipmentStatus.SHIPPED);
        ShipmentResponse serviceResponse = response();
        when(shipmentService.updateShipmentStatus(10, 8, request))
                .thenReturn(serviceResponse);

        var actual = new ShipmentController(shipmentService)
                .updateShipmentStatus(10, member, request);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(serviceResponse);
        verify(shipmentService).updateShipmentStatus(10, 8, request);
    }

    @Test
    void updateShipmentTrackingInfoUsesAuthenticatedMember() {
        ShipmentService shipmentService = mock(ShipmentService.class);
        AuthenticatedMember member = new AuthenticatedMember(8, "seller@example.com");
        UpdateShipmentTrackingInfoRequest request =
                new UpdateShipmentTrackingInfoRequest("Black Cat", "TRACK-2");
        ShipmentResponse serviceResponse = response();
        when(shipmentService.updateShipmentTrackingInfo(10, 8, request))
                .thenReturn(serviceResponse);

        var actual = new ShipmentController(shipmentService)
                .updateShipmentTrackingInfo(10, member, request);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(serviceResponse);
        verify(shipmentService).updateShipmentTrackingInfo(10, 8, request);
    }

    @Test
    void confirmDeliveryUsesAuthenticatedBuyer() {
        ShipmentService shipmentService = mock(ShipmentService.class);
        AuthenticatedMember member = new AuthenticatedMember(6, "buyer@example.com");
        ShipmentResponse serviceResponse = response();
        when(shipmentService.confirmDelivery(10, 6)).thenReturn(serviceResponse);

        var actual = new ShipmentController(shipmentService)
                .confirmDelivery(10, member);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(serviceResponse);
        verify(shipmentService).confirmDelivery(10, 6);
    }

    private ShipmentResponse response() {
        return new ShipmentResponse(
                20,
                10,
                "Black Cat",
                "TRACK-1",
                ShipmentStatus.PREPARING,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
