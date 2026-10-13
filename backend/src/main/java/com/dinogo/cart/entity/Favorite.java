package com.dinogo.cart.entity;

import com.dinogo.catalog.entity.Product;
import com.dinogo.member.entity.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "Favorite", schema = "cart")
public class Favorite {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "favorite_id")
	private int favoriteId;
	@ManyToOne
	@JoinColumn(name = "member_id")
	private Member member;
	@ManyToOne
	@JoinColumn(name = "product_id")
	private Product product;

	public void setMember(Member member) {
		this.member = member;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public int getFavoriteId() {
		return favoriteId;
	}

	public Member getMember() {
		return member;
	}

	public Product getProduct() {
		return product;
	}
}