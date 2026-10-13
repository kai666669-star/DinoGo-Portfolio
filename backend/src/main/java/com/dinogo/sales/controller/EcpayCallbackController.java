package com.dinogo.sales.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.Payment;
import com.dinogo.sales.entity.PaymentStatus;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.sales.repository.PaymentRepository;
import com.dinogo.sales.service.EcpayPaymentGateway;

@RestController
@RequestMapping("/api/ecpay")
public class EcpayCallbackController {
    private static final Logger log = LoggerFactory.getLogger(EcpayCallbackController.class);
    private final EcpayPaymentGateway gateway;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final String frontendBaseUrl;

    public EcpayCallbackController(
            EcpayPaymentGateway gateway,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.gateway = gateway;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    @PostMapping(value = "/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> callback(@RequestParam Map<String, String> fields) {
        String merchantTradeNo = fields.get("MerchantTradeNo");
        String tradeNo = fields.get("TradeNo");
        if (!gateway.verify(fields)) {
            log.warn("Rejected ECPay callback with an invalid CheckMacValue: MerchantTradeNo={}", merchantTradeNo);
            return ResponseEntity.badRequest().body("CheckMacValue Error");
        }
        if (merchantTradeNo == null || merchantTradeNo.isBlank()) {
            log.error("ECPay callback is missing MerchantTradeNo");
            return ResponseEntity.badRequest().body("Missing MerchantTradeNo");
        }
        Payment paymentReference = paymentRepository.findByPaymentNo(merchantTradeNo).orElse(null);
        if (paymentReference == null) {
            log.error("ECPay callback has no matching payment: MerchantTradeNo={}, TradeNo={}", merchantTradeNo,
                    tradeNo);
            return ResponseEntity.badRequest().body("Unknown MerchantTradeNo");
        }
        Order order = orderRepository.findForEcpayCallback(paymentReference.getOrder().getOrderId()).orElse(null);
        if (order == null) {
            log.error("ECPay callback has no matching order: MerchantTradeNo={}", merchantTradeNo);
            return ResponseEntity.badRequest().body("Unknown order");
        }
        Payment payment = order.getPayments().stream()
                .filter(candidate -> merchantTradeNo.equals(candidate.getPaymentNo()))
                .findFirst()
                .orElse(null);
        if (payment == null) {
            log.error("ECPay callback payment does not belong to locked order: MerchantTradeNo={}", merchantTradeNo);
            return ResponseEntity.badRequest().body("Payment mismatch");
        }
        BigDecimal tradeAmount;
        try {
            tradeAmount = new BigDecimal(fields.get("TradeAmt"));
        } catch (RuntimeException exception) {
            log.error("ECPay callback has an invalid TradeAmt: MerchantTradeNo={}, TradeNo={}", merchantTradeNo,
                    tradeNo);
            return ResponseEntity.badRequest().body("Invalid TradeAmt");
        }
        if (!"CREDIT_CARD".equals(payment.getPaymentMethod().getMethodCode())
                || tradeAmount.compareTo(payment.getAmount()) != 0) {
            log.error("ECPay callback payment mismatch: MerchantTradeNo={}, TradeNo={}", merchantTradeNo, tradeNo);
            return ResponseEntity.badRequest().body("Payment mismatch");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if (tradeNo != null && tradeNo.equals(payment.getTransactionNo()))
                return ResponseEntity.ok("1|OK");
            log.error("ECPay callback transaction mismatch for successful payment: MerchantTradeNo={}, TradeNo={}",
                    merchantTradeNo, tradeNo);
            return ResponseEntity.badRequest().body("Transaction mismatch");
        }
        if (payment.getStatus() == PaymentStatus.FAILED || payment.getStatus() == PaymentStatus.CANCELLED)
            return ResponseEntity.ok("1|OK");
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.error("ECPay callback has a non-processable payment status: MerchantTradeNo={}, status={}",
                    merchantTradeNo, payment.getStatus());
            return ResponseEntity.badRequest().body("Payment status mismatch");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("Ignored late ECPay callback for order in status {}: MerchantTradeNo={}",
                    order.getStatus(), merchantTradeNo);
            return ResponseEntity.ok("1|OK");
        }
        if ("1".equals(fields.get("RtnCode"))) {
            if (paymentRepository.existsByOrderOrderIdAndStatus(order.getOrderId(),
                    PaymentStatus.SUCCESS)) {
                log.error("ECPay callback would create a second successful payment: MerchantTradeNo={}",
                        merchantTradeNo);
                return ResponseEntity.badRequest().body("Duplicate successful payment");
            }
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionNo(tradeNo);
            payment.setPaidAt(LocalDateTime.now());
            order.setStatus(OrderStatus.PROCESSING);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            String message = fields.getOrDefault("RtnMsg", "ECPay payment failed");
            payment.setFailureReason(message.substring(0, Math.min(255, message.length())));
        }
        paymentRepository.save(payment);
        return ResponseEntity.ok("1|OK");
    }

    @PostMapping("/order-result")
    public ResponseEntity<Void> orderResult() {
        return ResponseEntity.status(303)
                .header("Location", frontendBaseUrl + "/member/orders")
                .build();
    }
}
