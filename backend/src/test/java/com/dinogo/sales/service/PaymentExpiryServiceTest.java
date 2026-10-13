package com.dinogo.sales.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dinogo.catalog.repository.ProductSkuRepository;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderItem;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.entity.Payment;
import com.dinogo.sales.entity.PaymentStatus;
import com.dinogo.sales.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class PaymentExpiryServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductSkuRepository productSkuRepository;

    @Test
    void expireCancelsPendingPaymentAndRestoresStockOnce() {
        Order order = new Order();
        order.setOrderId(10);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING);
        order.getPayments().add(payment);
        OrderItem item = new OrderItem();
        item.setSkuId(7);
        item.setQuantity(2);
        order.getOrderItems().add(item);
        when(orderRepository.findForPaymentExpiry(10)).thenReturn(Optional.of(order));
        when(productSkuRepository.restoreStock(7, 2)).thenReturn(1);

        new PaymentExpiryService(orderRepository, productSkuRepository).expire(10);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(PaymentStatus.CANCELLED, payment.getStatus());
        assertEquals("SYSTEM", order.getCancelledBy());
        assertEquals("PAYMENT_TIMEOUT", order.getCancelReason());
        verify(productSkuRepository).restoreStock(7, 2);
    }

    @Test
    void expiryIsIdempotentWhenCallbackOrCancellationAlreadyChangedOrder() {
        Order order = new Order();
        order.setStatus(OrderStatus.PROCESSING);
        when(orderRepository.findForPaymentExpiry(10)).thenReturn(Optional.of(order));

        new PaymentExpiryService(orderRepository, productSkuRepository).expire(10);

        verify(productSkuRepository, never()).restoreStock(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
