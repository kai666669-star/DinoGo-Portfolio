package com.dinogo.cart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.cart.entity.Favorite;
import com.dinogo.member.entity.Member;

public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {
    List<Favorite> findByMember(Member member);

    Optional<Favorite> findByMemberAndProduct_ProductId(
            Member member,
            Integer productId);

    void deleteByMemberAndProduct_ProductId(
            Member member,
            Integer productId);
}
