package com.dinogo.catalog.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuResponse {

    private Integer skuId;

    private Integer productId;

    private String spec1Name;

    private String spec1Value;

    private String spec2Name;

    private String spec2Value;

    private BigDecimal price;

    private Integer stock;

    private Byte status;
}