package com.dinogo.coupon.repository;

import com.dinogo.coupon.entity.MemberCoupon;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberCouponRepository
        extends JpaRepository<MemberCoupon, Integer> {

    boolean existsByMemberIdAndCouponId(
            Integer memberId,
            Integer couponId);

    Optional<MemberCoupon> findByMemberIdAndCouponId(
            Integer memberId,
            Integer couponId);

    Optional<MemberCoupon> findByMemberCouponIdAndMemberId(
            Integer memberCouponId,
            Integer memberId);

    List<MemberCoupon> findByMemberIdOrderByReceivedAtDesc(
            Integer memberId);

    long countByCouponId(Integer couponId);
}
