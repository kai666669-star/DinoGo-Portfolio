package com.dinogo.catalog.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSkuCreateRequest {

    private String spec1Name;

    private String spec1Value;

    private String spec2Name;
    private String spec2Value;

    @NotNull
    @DecimalMin(value = "1")
    private BigDecimal price;

    @NotNull
    @Min(0)
    private Integer stock;
}