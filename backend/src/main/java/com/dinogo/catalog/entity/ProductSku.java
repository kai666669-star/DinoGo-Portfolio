package com.dinogo.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ProductSku", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sku_id")
    private Integer skuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "spec1_name", length = 30)
    private String spec1Name;

    @Column(name = "spec1_value", length = 50)
    private String spec1Value;

    @Column(name = "spec2_name", length = 30)
    private String spec2Name;

    @Column(name = "spec2_value", length = 50)
    private String spec2Value;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    // 0 = 停用
    // 1 = 啟用
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Byte status = (byte) 1;

    @PrePersist
    protected void onCreate() {

        if (status == null) {
            status = (byte) 1;
        }
    }
}