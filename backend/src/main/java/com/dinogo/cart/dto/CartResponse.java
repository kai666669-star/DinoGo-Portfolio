package com.dinogo.cart.dto;

import java.util.List;

public record CartResponse(
		Integer cartId, 
		List<CartItemResponse> items) {
}
