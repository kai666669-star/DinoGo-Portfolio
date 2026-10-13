package com.dinogo.sales.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.catalog.repository.ProductSkuRepository;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.entity.PaymentStatus;
import com.dinogo.sales.exception.InvalidOrderException;
import com.dinogo.sales.repository.OrderRepository;

/** Expires unpaid orders and releases their previously reserved stock exactly once. */
@Service
public class PaymentExpiryService {

    private static final String PAYMENT_TIMEOUT_REASON = "PAYMENT_TIMEOUT";

    private final OrderRepository orderRepository;
    private final ProductSkuRepository productSkuRepository;

    public PaymentExpiryService(
            OrderRepository orderRepository,
            ProductSkuRepository productSkuRepository) {
        this.orderRepository = orderRepository;
        this.productSkuRepository = productSkuRepository;
    }

    @Transactional
    public void expire(Integer orderId) {
        Order order = orderRepository.findForPaymentExpiry(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return;
        }

        order.getPayments().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                .forEach(payment -> payment.setStatus(PaymentStatus.CANCELLED));

        order.getOrderItems().forEach(item -> {
            int restored = productSkuRepository.restoreStock(item.getSkuId(), item.getQuantity());
            if (restored == 0) {
                throw new InvalidOrderException("SKU does not exist: " + item.getSkuId());
            }
        });

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledBy("SYSTEM");
        order.setCancelReason(PAYMENT_TIMEOUT_REASON);
        order.setCancelledAt(LocalDateTime.now());
    }
}
