package com.dinogo.sales.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Payment", schema = "sales", uniqueConstraints = @UniqueConstraint(name = "uq_payment_payment_no", columnNames = "payment_no"), indexes = {
                @Index(name = "ix_payment_order_id", columnList = "order_id"),
                @Index(name = "ix_payment_payment_method_id", columnList = "payment_method_id"),
        @Index(name = "ix_payment_status", columnList = "status"),
        @Index(name = "ix_payment_order_status", columnList = "order_id, status"),
        @Index(name = "ix_payment_order_idempotency_key", columnList = "order_id, idempotency_key")
})
public class Payment {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "payment_id")
        private Integer paymentId;

        @Column(name = "payment_no", nullable = false, length = 40)
        private String paymentNo;

        /**
         * Client supplied key used to make payment creation safe to retry. Existing
         * rows created before this feature are intentionally allowed to remain null.
         */
        @Column(name = "idempotency_key", length = 64)
        private String idempotencyKey;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "order_id", nullable = false)
        private Order order;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "payment_method_id", nullable = false)
        private PaymentMethod paymentMethod;

        @Column(name = "amount", nullable = false, precision = 12, scale = 2)
        private BigDecimal amount;

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false, length = 20)
        private PaymentStatus status = PaymentStatus.PENDING;

        @Column(name = "transaction_no", length = 100)
        private String transactionNo;

        @Column(name = "failure_reason", length = 255)
        private String failureReason;

        @Column(name = "paid_at")
        private LocalDateTime paidAt;

        @CreationTimestamp
        @Column(name = "created_at", nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at", nullable = false)
        private LocalDateTime updatedAt;
}
