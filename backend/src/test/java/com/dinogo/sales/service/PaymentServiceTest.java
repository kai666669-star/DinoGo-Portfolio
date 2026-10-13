package com.dinogo.sales.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.dinogo.sales.dto.payment.SimulatePaymentRequest;
import com.dinogo.sales.dto.payment.CreatePaymentRequest;
import com.dinogo.sales.dto.payment.PaymentResponse;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.entity.Payment;
import com.dinogo.sales.entity.PaymentMethod;
import com.dinogo.sales.entity.PaymentStatus;
import com.dinogo.sales.exception.InvalidOrderException;
import com.dinogo.sales.exception.OrderNotFoundException;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.sales.repository.PaymentMethodRepository;
import com.dinogo.sales.repository.PaymentRepository;
import com.dinogo.sales.service.EcpayPaymentGateway;
import com.dinogo.salesii.event.OrderStatusChangedEvent;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private EcpayPaymentGateway ecpayPaymentGateway;
    @Mock
    private ApplicationEventPublisher events;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                paymentMethodRepository,
                orderRepository,
                events,
                true, ecpayPaymentGateway);
    }

    @Test
    void simulateSuccessMarksPaymentAndOrderPaid() {
        Payment payment = pendingPayment();
        when(paymentRepository.findForSimulation(20, 10, 1)).thenReturn(Optional.of(payment));
        when(paymentRepository.existsByOrderOrderIdAndStatus(10, PaymentStatus.SUCCESS))
                .thenReturn(false);
        when(paymentRepository.save(payment)).thenReturn(payment);

        var response = paymentService.simulatePaymentResult(
                10,
                20,
                1,
                new SimulatePaymentRequest(PaymentStatus.SUCCESS, null));

        assertEquals(PaymentStatus.SUCCESS, response.status());
        assertEquals(OrderStatus.PROCESSING, payment.getOrder().getStatus());
        assertNotNull(response.transactionNo());
        assertNotNull(response.paidAt());
        verify(paymentRepository).save(payment);
    }

    @Test
    void simulateFailureKeepsOrderPendingAndRecordsReason() {
        Payment payment = pendingPayment();
        when(paymentRepository.findForSimulation(20, 10, 1)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        var response = paymentService.simulatePaymentResult(
                10,
                20,
                1,
                new SimulatePaymentRequest(PaymentStatus.FAILED, "Card declined"));

        assertEquals(PaymentStatus.FAILED, response.status());
        assertEquals(OrderStatus.PENDING_PAYMENT, payment.getOrder().getStatus());
        assertEquals("Card declined", payment.getFailureReason());
        verify(paymentRepository).save(payment);
    }

    @Test
    void simulateRejectsUnsupportedResult() {
        Payment payment = pendingPayment();
        when(paymentRepository.findForSimulation(20, 10, 1)).thenReturn(Optional.of(payment));

        assertThrows(
                InvalidOrderException.class,
                () -> paymentService.simulatePaymentResult(
                        10,
                        20,
                        1,
                        new SimulatePaymentRequest(PaymentStatus.CANCELLED, null)));

        verify(paymentRepository, never()).save(payment);
    }

    @Test
    void simulateDoesNotExposeAnotherBuyersPayment() {
        when(paymentRepository.findForSimulation(20, 10, 99))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> paymentService.simulatePaymentResult(
                        10,
                        20,
                        99,
                        new SimulatePaymentRequest(PaymentStatus.SUCCESS, null)));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createPaymentUsesOrderTotalAndSelectedMethod() {
        Order order = pendingOrder();
        PaymentMethod method = paymentMethod();
        when(orderRepository.findForPaymentCreation(10, 1)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderIdAndStatus(10, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(paymentMethodRepository.findByMethodCode("CREDIT_CARD")).thenReturn(Optional.of(method));
        when(paymentRepository.save(org.mockito.ArgumentMatchers.any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createPayment(
                10,
                1,
                "payment-key-1",
                new CreatePaymentRequest("CREDIT_CARD"));

        assertEquals(new BigDecimal("1000.00"), response.amount());
        assertEquals(PaymentStatus.PENDING, response.status());
        assertEquals("CREDIT_CARD", response.paymentMethodCode());
        assertEquals(20, response.paymentNo().length());
        assertTrue(response.paymentNo().matches("PAY\\d{12}[A-Z0-9]{5}"));
    }

    @Test
    void disabledSimulationRejectsOnlinePaymentBeforeCreatingPendingPayment() {
        PaymentService disabledPaymentService = new PaymentService(
                paymentRepository,
                paymentMethodRepository,
                orderRepository,
                events,
                false, ecpayPaymentGateway);

        assertThrows(
                InvalidOrderException.class,
                () -> disabledPaymentService.createPayment(
                        10,
                        1,
                        "payment-key-2",
                        new CreatePaymentRequest("CREDIT_CARD")));

        verify(orderRepository, never()).findForPaymentCreation(any(), any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createCashOnDeliveryPaymentMovesOrderToProcessingAndKeepsPaymentPending() {
        Order order = pendingOrder();
        PaymentMethod method = paymentMethod();
        method.setMethodCode("CASH_ON_DELIVERY");
        method.setMethodName("貨到付款");

        when(orderRepository.findForPaymentCreation(10, 1)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderIdAndStatus(10, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(paymentMethodRepository.findByMethodCode("CASH_ON_DELIVERY"))
                .thenReturn(Optional.of(method));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.createPayment(
                10,
                1,
                "payment-key-3",
                new CreatePaymentRequest("CASH_ON_DELIVERY"));

        assertEquals(OrderStatus.PROCESSING, order.getStatus());
        assertEquals(PaymentStatus.PENDING, response.status());
        assertEquals("CASH_ON_DELIVERY", response.paymentMethodCode());
        verify(events).publishEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void createCashOnDeliveryRetryReturnsExistingPendingPayment() {
        Order order = pendingOrder();
        order.setStatus(OrderStatus.PROCESSING);

        Payment pendingPayment = pendingPayment();
        pendingPayment.getPaymentMethod().setMethodCode("CASH_ON_DELIVERY");
        when(orderRepository.findForPaymentCreation(10, 1)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderIdAndStatus(10, PaymentStatus.PENDING))
                .thenReturn(Optional.of(pendingPayment));

        PaymentResponse response = paymentService.createPayment(
                10,
                1,
                "payment-key-4",
                new CreatePaymentRequest("CASH_ON_DELIVERY"));

        assertEquals(pendingPayment.getPaymentId(), response.paymentId());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(events).publishEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void createPaymentRejectsAnotherPendingPayment() {
        Order order = pendingOrder();
        Payment pendingPayment = pendingPayment();
        pendingPayment.getPaymentMethod().setMethodCode("LINE_PAY");
        when(orderRepository.findForPaymentCreation(10, 1)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderIdAndStatus(10, PaymentStatus.PENDING))
                .thenReturn(Optional.of(pendingPayment));

        assertThrows(
                InvalidOrderException.class,
                () -> paymentService.createPayment(
                        10,
                        1,
                        "payment-key-5",
                        new CreatePaymentRequest("CREDIT_CARD")));

        verify(paymentRepository, never()).save(org.mockito.ArgumentMatchers.any(Payment.class));
    }

    @Test
    void createPaymentRetryCreatesNewEcpayTradeForExistingPendingPayment() {
        Order order = pendingOrder();
        Payment pendingPayment = pendingPayment();
        pendingPayment.setPaymentNo("PAY-EXISTING");
        when(orderRepository.findForPaymentCreation(10, 1)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderOrderIdAndStatus(10, PaymentStatus.PENDING))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentMethodRepository.findByMethodCode("CREDIT_CARD"))
                .thenReturn(Optional.of(paymentMethod()));
        when(ecpayPaymentGateway.isEnabled()).thenReturn(true);
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createPayment(
                10,
                1,
                "payment-key-6",
                new CreatePaymentRequest("CREDIT_CARD"));

        assertEquals(PaymentStatus.CANCELLED, pendingPayment.getStatus());
        assertEquals("Superseded by payment retry", pendingPayment.getFailureReason());
        assertTrue(!"PAY-EXISTING".equals(response.paymentNo()));
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void repeatedSuccessReturnsExistingResultWithoutMutation() {
        Payment payment = pendingPayment();
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionNo("SIM-EXISTING");
        payment.setPaidAt(java.time.LocalDateTime.of(2026, 8, 17, 10, 0));
        payment.getOrder().setStatus(OrderStatus.PAID);
        when(paymentRepository.findForSimulation(20, 10, 1)).thenReturn(Optional.of(payment));

        var response = paymentService.simulatePaymentResult(
                10,
                20,
                1,
                new SimulatePaymentRequest(PaymentStatus.SUCCESS, null));

        assertEquals("SIM-EXISTING", response.transactionNo());
        assertEquals(java.time.LocalDateTime.of(2026, 8, 17, 10, 0), response.paidAt());
        verify(paymentRepository, never()).save(payment);
    }

    @Test
    void createPaymentWithSameIdempotencyKeyReturnsExistingPayment() {
        Order order = pendingOrder();
        Payment existingPayment = pendingPayment();
        existingPayment.setIdempotencyKey("payment-key-existing");
        when(orderRepository.findForPaymentCreation(10, 1)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderOrderIdAndIdempotencyKey(10, "payment-key-existing"))
                .thenReturn(Optional.of(existingPayment));

        PaymentResponse response = paymentService.createPayment(
                10, 1, "payment-key-existing", new CreatePaymentRequest("CREDIT_CARD"));

        assertEquals(existingPayment.getPaymentId(), response.paymentId());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createPaymentRejectsIdempotencyKeyReusedForAnotherMethod() {
        Order order = pendingOrder();
        Payment existingPayment = pendingPayment();
        when(orderRepository.findForPaymentCreation(10, 1)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderOrderIdAndIdempotencyKey(10, "payment-key-existing"))
                .thenReturn(Optional.of(existingPayment));

        assertThrows(InvalidOrderException.class, () -> paymentService.createPayment(
                10, 1, "payment-key-existing", new CreatePaymentRequest("CASH_ON_DELIVERY")));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void repeatedFailureReturnsExistingResult() {
        Payment payment = pendingPayment();
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason("Card declined");
        when(paymentRepository.findForSimulation(20, 10, 1)).thenReturn(Optional.of(payment));

        var response = paymentService.simulatePaymentResult(
                10,
                20,
                1,
                new SimulatePaymentRequest(PaymentStatus.FAILED, "Different reason"));

        assertEquals("Card declined", response.failureReason());
        verify(paymentRepository, never()).save(payment);
    }

    @Test
    void terminalPaymentRejectsConflictingResult() {
        Payment payment = pendingPayment();
        payment.setStatus(PaymentStatus.FAILED);
        when(paymentRepository.findForSimulation(20, 10, 1)).thenReturn(Optional.of(payment));

        assertThrows(
                InvalidOrderException.class,
                () -> paymentService.simulatePaymentResult(
                        10,
                        20,
                        1,
                        new SimulatePaymentRequest(PaymentStatus.SUCCESS, null)));

        verify(paymentRepository, never()).save(payment);
    }

    @Test
    void pendingPaymentRejectsOrderThatIsNotAwaitingPayment() {
        Payment payment = pendingPayment();
        payment.getOrder().setStatus(OrderStatus.CANCELLED);
        when(paymentRepository.findForSimulation(20, 10, 1)).thenReturn(Optional.of(payment));

        assertThrows(
                InvalidOrderException.class,
                () -> paymentService.simulatePaymentResult(
                        10,
                        20,
                        1,
                        new SimulatePaymentRequest(PaymentStatus.SUCCESS, null)));

        verify(paymentRepository, never()).save(payment);
    }

    private Payment pendingPayment() {
        Order order = pendingOrder();
        PaymentMethod method = paymentMethod();

        Payment payment = new Payment();
        payment.setPaymentId(20);
        payment.setPaymentNo("PAY-TEST");
        payment.setOrder(order);
        payment.setPaymentMethod(method);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }

    private Order pendingOrder() {
        Order order = new Order();
        order.setOrderId(10);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(new BigDecimal("1000.00"));
        return order;
    }

    private PaymentMethod paymentMethod() {
        PaymentMethod method = new PaymentMethod();
        method.setMethodCode("CREDIT_CARD");
        return method;
    }
}
