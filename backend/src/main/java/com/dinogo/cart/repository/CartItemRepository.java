package com.dinogo.cart.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.cart.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
	List<CartItem> findByCartCartId(Integer cartId);

	CartItem findByCartCartIdAndProductSkuSkuId(int cartId, Integer skuId);

	List<CartItem> findByCartCartIdAndProductSkuSkuIdIn(
			Integer cartId,
			List<Integer> skuIds);
}
