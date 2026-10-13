package com.dinogo.cart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.cart.entity.Cart;
import com.dinogo.member.entity.Member;

public interface CartRepository extends JpaRepository<Cart, Integer> {
	Optional<Cart> findByMemberMemberId(Integer memberId);

	Optional<Cart> findByMember(Member member);

}
