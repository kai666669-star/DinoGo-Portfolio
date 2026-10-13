package com.dinogo.coupon.service;

import com.dinogo.coupon.dto.CouponResponse;
import com.dinogo.coupon.dto.MemberCouponResponse;
import com.dinogo.coupon.entity.Coupon;
import com.dinogo.coupon.entity.MemberCoupon;
import com.dinogo.coupon.repository.CouponRepository;
import com.dinogo.coupon.repository.MemberCouponRepository;
import com.dinogo.seller.repository.SellerRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberCouponService {

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final SellerRepository sellerRepository;

    public MemberCouponService(
            CouponRepository couponRepository,
            MemberCouponRepository memberCouponRepository,
            SellerRepository sellerRepository) {
        this.couponRepository = couponRepository;
        this.memberCouponRepository = memberCouponRepository;
        this.sellerRepository = sellerRepository;
    }

    @Transactional(readOnly = true)
    public List<MemberCouponResponse> getMemberCoupons(Integer memberId) {
        LocalDateTime now = LocalDateTime.now();

        return memberCouponRepository
                .findByMemberIdOrderByReceivedAtDesc(memberId)
                .stream()
                .map(memberCoupon -> {
                    Coupon coupon = couponRepository
                            .findById(memberCoupon.getCouponId())
                            .orElseThrow(() -> new IllegalStateException(
                                    "會員優惠券對應的優惠券不存在"));

                    return MemberCouponResponse.from(
                            memberCoupon,
                            coupon,
                            now,
                            sellerName(coupon));
                })
                .toList();
    }

    @Transactional
    public CouponResponse claimCoupon(
            Integer memberId,
            Integer couponId) {
        Coupon coupon = couponRepository
                .findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("找不到優惠券"));

        validateCouponCanBeClaimed(coupon);

        if (memberCouponRepository
                .existsByMemberIdAndCouponId(memberId, couponId)) {
            throw new IllegalArgumentException(
                    "此會員已領取過這張優惠券");
        }

        if (coupon.getLimitCount() != null) {
            long claimedCount = memberCouponRepository.countByCouponId(couponId);

            if (claimedCount >= coupon.getLimitCount()) {
                throw new IllegalArgumentException(
                        "此優惠券已達領取上限");
            }
        }

        MemberCoupon memberCoupon = new MemberCoupon();
        memberCoupon.setMemberId(memberId);
        memberCoupon.setCouponId(couponId);
        memberCoupon.setUsed(false);
        memberCoupon.setReceivedAt(LocalDateTime.now());

        memberCouponRepository.save(memberCoupon);

        return CouponResponse.from(coupon, sellerName(coupon));
    }

    private String sellerName(Coupon coupon) {
        return sellerRepository.findById(coupon.getSellerId())
                .map(seller -> seller.getStoreName())
                .orElse("未知賣家");
    }

    private void validateCouponCanBeClaimed(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();

        if ("DISABLED".equals(coupon.getStatus())) {
            throw new IllegalArgumentException(
                    "此優惠券已取消");
        }

        if (coupon.getStartAt().isAfter(now)
                || coupon.getEndAt().isBefore(now)) {
            throw new IllegalArgumentException(
                    "此優惠券不在可領取時間內");
        }
    }
}
