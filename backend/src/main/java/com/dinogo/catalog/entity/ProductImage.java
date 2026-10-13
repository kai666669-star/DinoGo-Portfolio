package com.dinogo.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ProductImage", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Integer imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_main", nullable = false)
    @Builder.Default
    private Boolean isMain = false;

    @PrePersist
    protected void onCreate() {

        if (sortOrder == null) {
            sortOrder = 0;
        }

        if (isMain == null) {
            isMain = false;
        }
    }
}