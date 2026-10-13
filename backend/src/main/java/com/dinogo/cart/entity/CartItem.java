package com.dinogo.cart.entity;

import com.dinogo.catalog.entity.ProductSku;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "CartItem", schema = "cart")
public class CartItem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "cart_item_id")
	private int cartItemId;
	@ManyToOne
	@JoinColumn(name = "cart_id")
	private Cart cart;
	@ManyToOne
	@JoinColumn(name = "sku_id")
	private ProductSku productSku;
	@Column(name = "quantity")
	private int quantity;

	public int getQuantity() {
		return quantity;
	}

	public void setCartItemId(int cartItemId) {
		this.cartItemId = cartItemId;
	}

	public void setProductSku(ProductSku productSku) {
		this.productSku = productSku;
	}

	public void setCart(Cart cart) {
		this.cart = cart;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public int getCartItemId() {
		return cartItemId;
	}
	@JsonIgnore
	public Cart getCart() {
		return cart;
	}

	public ProductSku getProductSku() {
		return productSku;
	}
}