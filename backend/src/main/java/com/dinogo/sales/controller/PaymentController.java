package com.dinogo.sales.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.sales.dto.payment.CreatePaymentRequest;
import com.dinogo.sales.dto.payment.PaymentResponse;
import com.dinogo.sales.dto.payment.SimulatePaymentRequest;
import com.dinogo.sales.service.PaymentService;
import com.dinogo.security.AuthenticatedMember;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders/{orderId}/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final boolean simulationEnabled;

    public PaymentController(
            PaymentService paymentService,
            @Value("${app.payment.simulation-enabled:false}") boolean simulationEnabled) {
        this.paymentService = paymentService;
        this.simulationEnabled = simulationEnabled;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @PathVariable Integer orderId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody CreatePaymentRequest request) {

        PaymentResponse response = paymentService.createPayment(
                orderId,
                member.memberId(),
                idempotencyKey,
                request);

        return ResponseEntity
                .created(java.net.URI.create("/api/orders/" + orderId + "/payments/" + response.paymentId()))
                .body(response);
    }

    @PostMapping("/{paymentId}/simulate")
    public ResponseEntity<PaymentResponse> simulatePaymentResult(
            @PathVariable Integer orderId,
            @PathVariable Integer paymentId,
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody SimulatePaymentRequest request) {

        if (!simulationEnabled) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(paymentService.simulatePaymentResult(
                orderId,
                paymentId,
                member.memberId(),
                request));
    }

}
