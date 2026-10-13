package com.dinogo.sales.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.entity.Payment;
import com.dinogo.sales.entity.PaymentMethod;
import com.dinogo.sales.entity.PaymentStatus;
import com.dinogo.sales.repository.PaymentRepository;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.sales.service.EcpayPaymentGateway;

import static org.mockito.Mockito.mock;

class EcpayCallbackControllerTest {

    @Test
    void successfulCallbackMarksPaymentAndOrderPaid() {
        EcpayPaymentGateway gateway = mock(EcpayPaymentGateway.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        Payment payment = pendingCreditCardPayment();
        payment.getOrder().getPayments().add(payment);
        Map<String, String> fields = Map.of(
                "MerchantTradeNo", "PAY260825120000ABCDE",
                "TradeNo", "ECPAY-TRADE-1",
                "TradeAmt", "1000",
                "RtnCode", "1",
                "CheckMacValue", "verified");
        when(gateway.verify(fields)).thenReturn(true);
        when(paymentRepository.findByPaymentNo("PAY260825120000ABCDE"))
                .thenReturn(Optional.of(payment));
        when(orderRepository.findForEcpayCallback(10)).thenReturn(Optional.of(payment.getOrder()));
        when(paymentRepository.existsByOrderOrderIdAndStatus(10, PaymentStatus.SUCCESS))
                .thenReturn(false);

        var response = new EcpayCallbackController(gateway, paymentRepository, orderRepository, "http://localhost:5173")
                .callback(fields);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("1|OK");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getTransactionNo()).isEqualTo("ECPAY-TRADE-1");
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(payment.getOrder().getStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(paymentRepository).save(payment);
    }

    @Test
    void invalidSignatureReturnsBadRequestWithoutReadingOrUpdatingPayment() {
        EcpayPaymentGateway gateway = mock(EcpayPaymentGateway.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        Map<String, String> fields = Map.of("MerchantTradeNo", "PAY260825120000ABCDE");
        when(gateway.verify(fields)).thenReturn(false);

        var response = new EcpayCallbackController(gateway, paymentRepository, orderRepository, "http://localhost:5173")
                .callback(fields);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("CheckMacValue Error");
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void lateSuccessCallbackForCancelledOrderAcknowledgesWithoutChangingState() {
        EcpayPaymentGateway gateway = mock(EcpayPaymentGateway.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        Payment payment = pendingCreditCardPayment();
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.getOrder().setStatus(OrderStatus.CANCELLED);
        payment.getOrder().getPayments().add(payment);
        Map<String, String> fields = Map.of(
                "MerchantTradeNo", "PAY260825120000ABCDE",
                "TradeNo", "ECPAY-TRADE-1",
                "TradeAmt", "1000",
                "RtnCode", "1",
                "CheckMacValue", "verified");
        when(gateway.verify(fields)).thenReturn(true);
        when(paymentRepository.findByPaymentNo("PAY260825120000ABCDE"))
                .thenReturn(Optional.of(payment));
        when(orderRepository.findForEcpayCallback(10)).thenReturn(Optional.of(payment.getOrder()));

        var response = new EcpayCallbackController(gateway, paymentRepository, orderRepository, "http://localhost:5173")
                .callback(fields);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("1|OK");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(payment.getOrder().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(paymentRepository, never()).save(payment);
    }

    private Payment pendingCreditCardPayment() {
        Order order = new Order();
        order.setOrderId(10);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        PaymentMethod method = new PaymentMethod();
        method.setMethodCode("CREDIT_CARD");
        Payment payment = new Payment();
        payment.setPaymentNo("PAY260825120000ABCDE");
        payment.setOrder(order);
        payment.setPaymentMethod(method);
        payment.setAmount(new BigDecimal("1000"));
        payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }
}
