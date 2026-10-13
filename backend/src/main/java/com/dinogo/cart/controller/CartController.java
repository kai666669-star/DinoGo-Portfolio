package com.dinogo.cart.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.cart.dto.CartItemRequest;
import com.dinogo.cart.dto.CartItemResponse;
import com.dinogo.cart.dto.CartResponse;
import com.dinogo.cart.entity.CartItem;
import com.dinogo.cart.service.CartService;
import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductSku;
import com.dinogo.security.AuthenticatedMember;

@RestController
@RequestMapping("/api/cart")
public class CartController {
	private final CartService cartService;

	public CartController(CartService cartService) {
		this.cartService = cartService;
	}

	// 取得購物車
	// @GetMapping("/{memberId}")
	// public Cart getCart(@PathVariable Integer memberId) {
	//
	// return cartService.getOrCreateCart(memberId);
	// }

	// 帳號
	@GetMapping
	public CartResponse getCart(Authentication authentication) {

		AuthenticatedMember principal = (AuthenticatedMember) authentication.getPrincipal();

		String email = principal.email();

		return cartService.getCart(email);
	}
	// 新增
	// @PostMapping("/{memberId}/items")
	// public CartItemResponse addItem(
	// @PathVariable Integer memberId,
	// @RequestBody CartItemRequest dto) {
	//
	// CartItem item = cartService.addItem(
	// memberId,
	// dto.skuId(),
	// dto.quantity());
	// return new CartItemResponse(
	// item.getCartItemId(),
	// item.getProductSku().getSkuId(),
	// item.getQuantity());
	// }

	// 帳號
	@PostMapping("/items")
	public CartItemResponse addItem(
			Authentication authentication,
			@RequestBody CartItemRequest dto) {

		String email = getEmail(authentication);

		return cartService.addItem(
				email,
				dto.skuId(),
				dto.quantity());
	}
	// 修改
	// @PutMapping("/items/{cartItemId}")
	// public CartItemResponse updateQuantity(
	// @PathVariable Integer cartItemId,
	// @RequestBody CartItemRequest dto) {
	//
	// CartItem item = cartService.updateQuantity(
	// cartItemId,
	// dto.quantity());
	//
	// return new CartItemResponse(
	// item.getCartItemId(),
	// item.getProductSku().getSkuId(),
	// item.getQuantity());
	// }

	// 帳號
	@PutMapping("/items/{cartItemId}")
	public CartItemResponse updateQuantity(
			Authentication authentication,
			@PathVariable Integer cartItemId,
			@RequestBody CartItemRequest dto) {

		String email = getEmail(authentication);

		return cartService.updateQuantity(
				email,
				cartItemId,
				dto);
	}
	// 刪除單筆
	// @DeleteMapping("/items/{cartItemId}")
	// public void deleteItem(
	// @PathVariable Integer cartItemId) {
	//
	// cartService.deleteItem(cartItemId);
	// }

	// 帳號
	@DeleteMapping("/items/{cartItemId}")
	public void deleteItem(Authentication authentication, @PathVariable Integer cartItemId) {

		// Integer memberId = getMemberId(authentication);
		// cartService.deleteItem(memberId, cartItemId);
		String email = getEmail(authentication);
		cartService.deleteItem(email, cartItemId);
	}

	// 清空購物車
	// @DeleteMapping("/{cartId}/items")
	// public void clearCart(
	// @PathVariable Integer cartId) {
	//
	// cartService.clearCart(cartId);
	// }
	// 帳號
	@DeleteMapping("/items")
	public void clearCart(Authentication authentication) {

		// Integer memberId = getMemberId(authentication);
		// cartService.clearCart(memberId);
		String email = getEmail(authentication);
		cartService.clearCart(email);
	}

	private String getEmail(Authentication authentication) {

		AuthenticatedMember principal = (AuthenticatedMember) authentication.getPrincipal();

		return principal.email();
	}
}
