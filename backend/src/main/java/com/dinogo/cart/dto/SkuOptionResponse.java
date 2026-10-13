package com.dinogo.cart.dto;

import java.math.BigDecimal;

public record SkuOptionResponse(
                Integer skuId,
                String skuName,
                BigDecimal price,
                Byte status) {
}