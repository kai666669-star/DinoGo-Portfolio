package com.dinogo.sales.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.SecureRandom;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;

import com.dinogo.sales.dto.payment.CreatePaymentRequest;
import com.dinogo.sales.dto.payment.PaymentResponse;
import com.dinogo.sales.dto.payment.EcpayCheckout;
import com.dinogo.sales.dto.payment.SimulatePaymentRequest;
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
import com.dinogo.salesii.event.OrderStatusChangedEvent;

@Service
public class PaymentService {

    private static final String CASH_ON_DELIVERY = "CASH_ON_DELIVERY";
    private static final String PAYMENT_NO_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int PAYMENT_NO_RETRY_LIMIT = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher events;
    private final boolean simulationEnabled;
    private final EcpayPaymentGateway ecpayPaymentGateway;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentMethodRepository paymentMethodRepository,
            OrderRepository orderRepository,
            ApplicationEventPublisher events,
            @Value("${app.payment.simulation-enabled:false}") boolean simulationEnabled,
            EcpayPaymentGateway ecpayPaymentGateway) {
        this.paymentRepository = paymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.orderRepository = orderRepository;
        this.events = events;
        this.simulationEnabled = simulationEnabled;
        this.ecpayPaymentGateway = ecpayPaymentGateway;
    }

    @Transactional
    public PaymentResponse createPayment(
            Integer orderId,
            Integer buyerId,
            String idempotencyKey,
            CreatePaymentRequest request) {

        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);

        if (!simulationEnabled && !CASH_ON_DELIVERY.equals(request.paymentMethodCode())
                && !("CREDIT_CARD".equals(request.paymentMethodCode()) && ecpayPaymentGateway.isEnabled())) {
            throw new InvalidOrderException(
                    "Online payment is not available in this environment");
        }

        Order order = orderRepository
                .findForPaymentCreation(orderId, buyerId)
                .orElseThrow(() ->
                        new OrderNotFoundException("Order does not exist"));

        Payment existingPayment = paymentRepository
                .findByOrderOrderIdAndIdempotencyKey(orderId, normalizedIdempotencyKey)
                .orElse(null);
        if (existingPayment != null) {
            if (!existingPayment.getPaymentMethod().getMethodCode().equals(request.paymentMethodCode())) {
                throw new InvalidOrderException("Idempotency-Key was already used with a different payment method");
            }
            publishCashOnDeliveryOrderCreated(request.paymentMethodCode(), orderId);
            return toResponse(existingPayment, checkoutFor(existingPayment));
        }

        Payment pendingPayment = paymentRepository
                .findFirstByOrderOrderIdAndStatus(orderId, PaymentStatus.PENDING)
                .orElse(null);

        boolean cashOnDeliveryRetry = order.getStatus() == OrderStatus.PROCESSING
                && CASH_ON_DELIVERY.equals(request.paymentMethodCode())
                && pendingPayment != null
                && CASH_ON_DELIVERY.equals(
                        pendingPayment.getPaymentMethod().getMethodCode());

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && !cashOnDeliveryRetry) {
            throw new InvalidOrderException(
                    "Only pending-payment orders can create a payment");
        }

        if (pendingPayment != null) {
            if (pendingPayment.getPaymentMethod().getMethodCode()
                    .equals(request.paymentMethodCode())) {
                if (isEcpayCreditCardRetry(pendingPayment)) {
                    pendingPayment.setStatus(PaymentStatus.CANCELLED);
                    pendingPayment.setFailureReason("Superseded by payment retry");
                    paymentRepository.save(pendingPayment);
                } else {
                    publishCashOnDeliveryOrderCreated(request.paymentMethodCode(), orderId);
                    return toResponse(pendingPayment, checkoutFor(pendingPayment));
                }
            }
            if (!pendingPayment.getPaymentMethod().getMethodCode()
                    .equals(request.paymentMethodCode())) {
                throw new InvalidOrderException(
                        "A pending payment with a different method already exists for this order");
            }
        }

        PaymentMethod method = paymentMethodRepository
                .findByMethodCode(request.paymentMethodCode())
                .orElseThrow(() ->
                        new InvalidOrderException(
                                "Payment method does not exist: "
                                        + request.paymentMethodCode()));

        Payment payment = new Payment();
        payment.setPaymentNo(generatePaymentNo());
        payment.setIdempotencyKey(normalizedIdempotencyKey);
        payment.setOrder(order);
        payment.setPaymentMethod(method);

        // 金額只能使用後端訂單總金額。
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);

        if (CASH_ON_DELIVERY.equals(method.getMethodCode())) {
            order.setStatus(OrderStatus.PROCESSING);
        }

        Payment savedPayment = paymentRepository.save(payment);
        publishCashOnDeliveryOrderCreated(method.getMethodCode(), order.getOrderId());
        return toResponse(savedPayment, checkoutFor(savedPayment));
    }

    private void publishCashOnDeliveryOrderCreated(String paymentMethodCode, Integer orderId) {
        if (CASH_ON_DELIVERY.equals(paymentMethodCode)) {
            events.publishEvent(new OrderStatusChangedEvent(orderId));
        }
    }

    @Transactional
    public PaymentResponse simulatePaymentResult(
            Integer orderId,
            Integer paymentId,
            Integer buyerId,
            SimulatePaymentRequest request) {

        Payment payment = paymentRepository
                .findForSimulation(paymentId, orderId, buyerId)
                .orElseThrow(() -> new OrderNotFoundException("Payment does not exist"));

        if (request.status() != PaymentStatus.SUCCESS
                && request.status() != PaymentStatus.FAILED) {
            throw new InvalidOrderException("Simulated payment result must be SUCCESS or FAILED");
        }

        if (payment.getStatus() == request.status()) {
            return toResponse(payment);
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidOrderException("Payment already has a different result");
        }

        Order order = payment.getOrder();
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderException("Order is not awaiting payment");
        }

        if (request.status() == PaymentStatus.SUCCESS) {
            boolean successExists = paymentRepository.existsByOrderOrderIdAndStatus(
                    orderId,
                    PaymentStatus.SUCCESS);
            if (successExists) {
                throw new InvalidOrderException("A successful payment already exists for this order");
            }

            LocalDateTime paidAt = LocalDateTime.now();
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionNo(generateTransactionNo());
            payment.setFailureReason(null);
            payment.setPaidAt(paidAt);
            order.setStatus(OrderStatus.PROCESSING);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(normalizeFailureReason(request.failureReason()));
        }

        return toResponse(paymentRepository.save(payment));
    }

    private String generatePaymentNo() {
        for (int attempt = 0; attempt < PAYMENT_NO_RETRY_LIMIT; attempt++) {
            StringBuilder paymentNo = new StringBuilder("PAY")
                    .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")));
            for (int index = 0; index < 5; index++) {
                paymentNo.append(PAYMENT_NO_ALPHABET.charAt(SECURE_RANDOM.nextInt(PAYMENT_NO_ALPHABET.length())));
            }
            if (!paymentRepository.existsByPaymentNo(paymentNo.toString())) {
                return paymentNo.toString();
            }
        }
        throw new InvalidOrderException("Unable to generate a unique payment number");
    }

    private String generateTransactionNo() {
        return "SIM-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private String normalizeFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return "Simulated payment failed";
        }
        String normalized = failureReason.trim();
        return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
    }

    private boolean isEcpayCreditCardRetry(Payment pendingPayment) {
        return ecpayPaymentGateway.isEnabled()
                && "CREDIT_CARD".equals(pendingPayment.getPaymentMethod().getMethodCode());
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidOrderException("Idempotency-Key is required");
        }
        String normalized = idempotencyKey.trim();
        if (normalized.length() > 64) {
            throw new InvalidOrderException("Idempotency-Key must not exceed 64 characters");
        }
        return normalized;
    }

    private EcpayCheckout checkoutFor(Payment payment) {
        return "CREDIT_CARD".equals(payment.getPaymentMethod().getMethodCode()) && ecpayPaymentGateway.isEnabled()
                ? ecpayPaymentGateway.checkout(payment) : null;
    }

    private PaymentResponse toResponse(Payment payment) { return toResponse(payment, null); }
    private PaymentResponse toResponse(Payment payment, EcpayCheckout ecpayCheckout) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getPaymentNo(),
                payment.getOrder().getOrderId(),
                payment.getPaymentMethod().getMethodCode(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getTransactionNo(),
                payment.getFailureReason(),
                payment.getPaidAt(),
                payment.getCreatedAt(), ecpayCheckout);
    }
}
