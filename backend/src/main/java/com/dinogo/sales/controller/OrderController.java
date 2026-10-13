package com.dinogo.sales.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.sales.dto.CancelOrderRequest;
import com.dinogo.sales.dto.OrderDetailResponse;
import com.dinogo.sales.dto.OrderListResponse;
import com.dinogo.sales.dto.UpdateOrderStatusRequest;
import com.dinogo.sales.dto.order.CreateOrderRequest;
import com.dinogo.sales.dto.order.CreateOrderResponse;
import com.dinogo.security.AuthenticatedMember;
import com.dinogo.sales.service.OrderService;

import jakarta.validation.Valid;

/** 訂單 REST API。 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody CreateOrderRequest request) {
        CreateOrderResponse response = orderService.createOrder(request, member.memberId());
        return ResponseEntity.created(URI.create("/api/orders/" + response.orderId())).body(response);
    }

    @GetMapping("/member")
    public ResponseEntity<List<OrderListResponse>> getMemberOrders(
            @AuthenticationPrincipal AuthenticatedMember member) {

        List<OrderListResponse> response = orderService.getMemberOrders(member.memberId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getMemberOrder(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal AuthenticatedMember member) {
        OrderDetailResponse response = orderService.getMemberOrder(orderId, member.memberId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderDetailResponse> updateOrderStatus(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        OrderDetailResponse response = orderService.updateStatusBySeller(
                orderId,
                member.memberId(),
                request.status(),
                request.reason());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDetailResponse> cancelOrder(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody CancelOrderRequest request) {

        OrderDetailResponse response = orderService.cancelOrder(
                orderId,
                member.memberId(),
                request.reason());

        return ResponseEntity.ok(response);
    }
}
