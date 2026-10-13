package com.dinogo.sales.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "OrderItem", schema = "sales", indexes = {
                @Index(name = "ix_order_item_order_id", columnList = "order_id"),
                @Index(name = "ix_order_item_product_id", columnList = "product_id"),
                @Index(name = "ix_order_item_sku_id", columnList = "sku_id"),
                @Index(name = "ix_order_item_is_reviewed", columnList = "is_reviewed")
})
public class OrderItem {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "order_item_id")
        private Integer orderItemId;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "order_id", nullable = false)
        private Order order;

        @Column(name = "product_id", nullable = false)
        private Integer productId;

        @Column(name = "sku_id", nullable = false)
        private Integer skuId;

        @Column(name = "product_name", nullable = false, length = 100)
        private String productName;

        @Column(name = "sku_spec", length = 200)
        private String skuSpec;

        @Column(name = "product_image_url", length = 500)
        private String productImageUrl;

        @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
        private BigDecimal unitPrice;

        @Column(name = "quantity", nullable = false)
        private Integer quantity;

        @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
        private BigDecimal subtotal;

        @Column(name = "is_reviewed", nullable = false)
        private Boolean isReviewed = false;

        @CreationTimestamp
        @Column(name = "created_at", nullable = false, updatable = false)
        private LocalDateTime createdAt;
}
