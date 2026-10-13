package com.dinogo.catalog.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductUpdateRequest {

    private Integer subcategoryId;

    private Integer brandId;

    @Size(max = 50)
    private String productName;

    @Size(max = 3000)
    private String description;

    @DecimalMin(value = "1")
    private BigDecimal basePrice;

    private Byte status;
}