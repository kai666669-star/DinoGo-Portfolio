package com.dinogo.sales.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.dinogo.sales.dto.order.CreateOrderItemRequest;
import com.dinogo.sales.dto.order.CreateOrderRequest;
import com.dinogo.sales.dto.order.CreateOrderResponse;
import com.dinogo.sales.dto.UpdateOrderStatusRequest;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.service.OrderService;
import com.dinogo.security.AuthenticatedMember;

class OrderControllerTest {

    @Test
    void getMemberOrderUsesAuthenticatedMemberIdInsteadOfAnotherMemberId() {
        OrderService orderService = mock(OrderService.class);
        AuthenticatedMember authenticatedMember = new AuthenticatedMember(6, "buyer@example.com");
        OrderController controller = new OrderController(orderService);

        controller.getMemberOrder(1, authenticatedMember);

        verify(orderService).getMemberOrder(1, 6);
        verify(orderService, never()).getMemberOrder(1, 7);
    }

    @Test
    void createOrderReturnsCreatedResponseAndLocation() {
        OrderService orderService = mock(OrderService.class);
        CreateOrderRequest request = new CreateOrderRequest(
                10, "HOME_DELIVERY", null, null,
                List.of(new CreateOrderItemRequest(100, 1)));
        CreateOrderResponse serviceResponse = new CreateOrderResponse(
                99, "ORD202608110001", OrderStatus.PENDING_PAYMENT,
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, null);
        AuthenticatedMember member = new AuthenticatedMember(1, "buyer@example.com");
        when(orderService.createOrder(request, 1)).thenReturn(serviceResponse);

        ResponseEntity<CreateOrderResponse> response =
                new OrderController(orderService).createOrder(member, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).hasToString("/api/orders/99");
        assertThat(response.getBody()).isSameAs(serviceResponse);
        verify(orderService).createOrder(request, 1);
    }

    @Test
    void updateOrderStatusUsesAuthenticatedMemberId() {
        OrderService orderService = mock(OrderService.class);
        AuthenticatedMember member = new AuthenticatedMember(6, "seller@example.com");
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(
                OrderStatus.PROCESSING, null);

        new OrderController(orderService).updateOrderStatus(99, member, request);

        verify(orderService).updateStatusBySeller(
                99, 6, OrderStatus.PROCESSING, null);
    }
}
