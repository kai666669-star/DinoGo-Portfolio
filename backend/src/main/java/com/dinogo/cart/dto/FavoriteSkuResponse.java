package com.dinogo.cart.dto;

public record FavoriteSkuResponse(
        Integer skuId,
        String skuName,
        Byte skuStatus,
        Integer skuStock,
        Boolean available) {
}
