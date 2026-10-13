package com.dinogo.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartItemResponse(
		Integer cartItemId,
		Integer skuId,
		Integer productId, // ⭐ 新增
		String productName,
		BigDecimal price,
		Integer quantity,
		Integer stock, // ⭐ 新增
		String productImage,
		Integer sellerId,
		String storeName,
		List<SkuOptionResponse> skus,
		boolean available,
		String unavailableReason) {
}
