package com.dinogo.cart.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.cart.repository.CartItemRepository;

@Service
@Transactional
public class CartItemService {
	private final CartItemRepository cartItemRepository;

	public CartItemService(CartItemRepository cartItemRepository) {
		super();
		this.cartItemRepository = cartItemRepository;
	}
}
