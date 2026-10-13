package com.dinogo.seller.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(schema = "seller", name = "withdrawal_request")
@DynamicInsert
public class WithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "withdrawal_id")
    private Integer withdrawalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WithdrawalStatus status;

    @Generated(event = EventType.INSERT)
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    protected WithdrawalRequest() {
    }

    public WithdrawalRequest(Seller seller, BigDecimal amount) {
        this.seller = seller;
        this.amount = amount;
        this.status = WithdrawalStatus.PROCESSING;
    }

    public Integer getWithdrawalId() {
        return withdrawalId;
    }

    public Seller getSeller() {
        return seller;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public WithdrawalStatus getStatus() {
        return status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }
}
