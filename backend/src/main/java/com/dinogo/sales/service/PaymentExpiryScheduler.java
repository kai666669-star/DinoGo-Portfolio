package com.dinogo.sales.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.repository.OrderRepository;

@Component
@ConditionalOnProperty(
        name = "app.payment-expiry.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PaymentExpiryScheduler {

    private final OrderRepository orderRepository;
    private final PaymentExpiryService paymentExpiryService;
    private final long timeoutMinutes;

    public PaymentExpiryScheduler(
            OrderRepository orderRepository,
            PaymentExpiryService paymentExpiryService,
            @Value("${app.payment-expiry.timeout-minutes:15}") long timeoutMinutes) {
        this.orderRepository = orderRepository;
        this.paymentExpiryService = paymentExpiryService;
        this.timeoutMinutes = timeoutMinutes;
    }

    @Scheduled(
            initialDelayString = "${app.payment-expiry.initial-delay-ms:60000}",
            fixedDelayString = "${app.payment-expiry.fixed-delay-ms:60000}")
    public void expirePendingPayments() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        orderRepository.findOrderIdsForPaymentExpiry(OrderStatus.PENDING_PAYMENT, deadline)
                .forEach(paymentExpiryService::expire);
    }
}
