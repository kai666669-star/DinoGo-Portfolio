package com.dinogo.sales.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Shipment", schema = "sales", uniqueConstraints = @UniqueConstraint(name = "uq_shipment_order", columnNames = "order_id"), indexes = {
                @Index(name = "ix_shipment_status", columnList = "status"),
                @Index(name = "ix_shipment_tracking_no", columnList = "tracking_no")
})
public class Shipment {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "shipment_id")
        private Integer shipmentId;

        @OneToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "order_id", nullable = false)
        private Order order;

        @Column(name = "carrier_name", length = 100)
        private String carrierName;

        @Column(name = "tracking_no", length = 100)
        private String trackingNo;

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false, length = 30)
        private ShipmentStatus status = ShipmentStatus.PREPARING;

        @Column(name = "shipped_at")
        private LocalDateTime shippedAt;

        @Column(name = "available_pickup_at")
        private LocalDateTime availablePickupAt;

        @Column(name = "delivered_at")
        private LocalDateTime deliveredAt;

        @Column(name = "delivery_photo_url", length = 500)
        private String deliveryPhotoUrl;

        @CreationTimestamp
        @Column(name = "created_at", nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at", nullable = false)
        private LocalDateTime updatedAt;

}
