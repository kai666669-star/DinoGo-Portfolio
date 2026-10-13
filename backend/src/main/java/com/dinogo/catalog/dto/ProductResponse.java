package com.dinogo.catalog.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ProductResponse {

    private Integer productId;
    private Integer sellerId;
    private Integer subcategoryId;
    private Integer brandId;
    private String productName;
    private String description;
    private BigDecimal basePrice;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private Integer stock;
    private String imageUrl;
    private Byte status;

    private Integer soldCount;

}