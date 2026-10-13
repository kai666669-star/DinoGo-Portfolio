package com.dinogo.sales.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.dinogo.sales.entity.Payment;
import com.dinogo.sales.entity.PaymentStatus;


public interface PaymentRepository extends JpaRepository<Payment, Integer> {
        List<Payment> findByOrderOrderIdOrderByCreatedAtDesc(Integer orderId);

        boolean existsByOrderOrderIdAndStatus(Integer orderId, PaymentStatus status);

        boolean existsByPaymentNo(String paymentNo);

        @EntityGraph(attributePaths = "paymentMethod")
        Optional<Payment> findFirstByOrderOrderIdAndStatus(
                        Integer orderId,
                        PaymentStatus status);

        @EntityGraph(attributePaths = "paymentMethod")
        Optional<Payment> findFirstByOrderOrderIdAndStatusAndPaymentMethodMethodCode(
                        Integer orderId,
                        PaymentStatus status,
                        String methodCode);

        Optional<Payment> findByOrderOrderIdAndIdempotencyKey(
                        Integer orderId,
                        String idempotencyKey);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @EntityGraph(attributePaths = { "order", "paymentMethod" })
        @Query("""
                        SELECT payment
                        FROM Payment payment
                        WHERE payment.paymentId = :paymentId
                          AND payment.order.orderId = :orderId
                          AND payment.order.buyerId = :buyerId
                        """)
        Optional<Payment> findForSimulation(
                        @Param("paymentId") Integer paymentId,
                        @Param("orderId") Integer orderId,
                        @Param("buyerId") Integer buyerId);

        @EntityGraph(attributePaths = { "order", "paymentMethod" })
        Optional<Payment> findByPaymentNo(String paymentNo);
}
