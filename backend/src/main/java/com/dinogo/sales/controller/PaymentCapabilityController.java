package com.dinogo.sales.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.sales.dto.payment.PaymentCapabilityResponse;
import com.dinogo.sales.dto.payment.PaymentMethodResponse;
import com.dinogo.sales.service.PaymentMethodService;

@RestController
@RequestMapping("/api/payments")
public class PaymentCapabilityController {

    private final boolean simulationEnabled;
    private final PaymentMethodService paymentMethodService;

    public PaymentCapabilityController(
            @Value("${app.payment.simulation-enabled:false}") boolean simulationEnabled,
            PaymentMethodService paymentMethodService) {
        this.simulationEnabled = simulationEnabled;
        this.paymentMethodService = paymentMethodService;
    }

    @GetMapping("/capabilities")
    public PaymentCapabilityResponse getCapabilities() {
        return new PaymentCapabilityResponse(simulationEnabled);
    }

    @GetMapping("/methods")
    public List<PaymentMethodResponse> getPaymentMethods() {
        return paymentMethodService.getPaymentMethods();
    }
}
