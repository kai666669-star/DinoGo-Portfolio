package com.dinogo.coupon.repository;

import com.dinogo.coupon.entity.Coupon;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Integer> {

    List<Coupon> findBySellerIdOrderByCouponIdDesc(Integer sellerId);

    Optional<Coupon> findBySellerIdAndCouponId(Integer sellerId, Integer couponId);

    Optional<Coupon> findBySellerIdAndCouponCode(Integer sellerId, String couponCode);

    List<Coupon> findAllByOrderByCouponIdDesc();

    List<Coupon> findByStatusOrderByCouponIdDesc(String status);
}
