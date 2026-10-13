package com.dinogo.sales.entity;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "ShipmentEvent", schema = "sales", indexes = @Index(name = "ix_shipment_event_shipment_occurred", columnList = "shipment_id,occurred_at"))
public class ShipmentEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipment_event_id") private Integer shipmentEventId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false) private Shipment shipment;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false, length = 30) private ShipmentEventType eventType;
    @Enumerated(EnumType.STRING) @Column(name = "source", nullable = false, length = 20) private ShipmentEventSource source;
    @Column(name = "remark", length = 500) private String remark;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
}
