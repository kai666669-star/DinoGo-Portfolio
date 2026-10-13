package com.dinogo.coupon.service;

import com.dinogo.catalog.entity.Product;
import com.dinogo.coupon.entity.Coupon;
import com.dinogo.coupon.entity.MemberCoupon;
import com.dinogo.coupon.repository.CouponRepository;
import com.dinogo.coupon.repository.MemberCouponRepository;
import com.dinogo.sales.repository.OrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CouponUsageService {

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final OrderRepository orderRepository;

    public CouponUsageService(
            CouponRepository couponRepository,
            MemberCouponRepository memberCouponRepository,
            OrderRepository orderRepository) {
        this.couponRepository = couponRepository;
        this.memberCouponRepository = memberCouponRepository;
        this.orderRepository = orderRepository;
    }

    public AppliedCoupon validateAndCalculate(
            Integer memberCouponId,
            Integer memberId,
            Integer sellerId,
            BigDecimal subtotal,
            List<CouponItem> items) {
        MemberCoupon memberCoupon = memberCouponRepository
                .findByMemberCouponIdAndMemberId(memberCouponId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("您尚未領取此優惠券"));

        Coupon coupon = couponRepository.findById(memberCoupon.getCouponId())
                .orElseThrow(() -> new IllegalArgumentException("優惠券不存在"));
        validatePerMemberUsage(memberCoupon, coupon, memberId);
        LocalDateTime now = LocalDateTime.now();

        if ("DISABLED".equals(coupon.getStatus())) {
            throw new IllegalArgumentException("此優惠券目前無法使用");
        }
        if (coupon.getStartAt().isAfter(now)) {
            throw new IllegalArgumentException("優惠券尚未開始");
        }
        if (coupon.getEndAt().isBefore(now)) {
            throw new IllegalArgumentException("優惠券已過期");
        }

        BigDecimal applicableAmount = findApplicableAmount(coupon, sellerId, items);
        if (applicableAmount.signum() <= 0) {
            throw new IllegalArgumentException("此優惠券不適用於本次訂單商品");
        }
        if (coupon.getMinPurchaseAmount() != null
                && applicableAmount.compareTo(coupon.getMinPurchaseAmount()) < 0) {
            throw new IllegalArgumentException("尚未達到優惠券最低消費金額");
        }

        BigDecimal discount = calculateDiscount(coupon, applicableAmount);
        return new AppliedCoupon(memberCoupon, coupon, discount);
    }

    public void consume(AppliedCoupon appliedCoupon) {
        MemberCoupon memberCoupon = appliedCoupon.memberCoupon();
        Coupon coupon = appliedCoupon.coupon();

        if (!Boolean.TRUE.equals(memberCoupon.getUsed())) {
            memberCoupon.setUsed(true);
            memberCoupon.setUsedAt(LocalDateTime.now());
        }
        coupon.setUsedCount((coupon.getUsedCount() == null ? 0 : coupon.getUsedCount()) + 1);

        memberCouponRepository.save(memberCoupon);
        couponRepository.save(coupon);
    }

    private BigDecimal findApplicableAmount(
            Coupon coupon,
            Integer sellerId,
            List<CouponItem> items) {
        if (coupon.getSellerId() == null
                || sellerId == null
                || !coupon.getSellerId().equals(sellerId)) {
            return BigDecimal.ZERO;
        }

        return switch (coupon.getScopeType()) {
            case "ALL" -> items.stream()
                    .map(CouponItem::subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case "STORE" -> {
                yield items.stream()
                        .map(CouponItem::subtotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            case "CATEGORY" -> items.stream()
                    .filter(item -> item.product() != null
                            && item.product().getSubcategory() != null
                            && item.product().getSubcategory().getCategory() != null
                            && coupon.getCategoryId().equals(
                                    item.product().getSubcategory().getCategory().getCategoryId()))
                    .map(CouponItem::subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case "PRODUCT" -> items.stream()
                    .filter(item -> item.product() != null
                            && coupon.getProductId().equals(item.product().getProductId()))
                    .map(CouponItem::subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            default -> throw new IllegalArgumentException("不支援的優惠券適用範圍");
        };
    }

    private void validatePerMemberUsage(
            MemberCoupon memberCoupon,
            Coupon coupon,
            Integer memberId) {
        String usagePolicy = coupon.getPerMemberUsagePolicy() == null
                ? "ONCE"
                : coupon.getPerMemberUsagePolicy();

        if ("REPEAT".equals(usagePolicy)) {
            return;
        }

        long usedTimes = orderRepository.countByBuyerIdAndMemberCouponId(
                memberId,
                memberCoupon.getMemberCouponId());
        if (Boolean.TRUE.equals(memberCoupon.getUsed()) || usedTimes > 0) {
            throw new IllegalArgumentException("此優惠券已使用");
        }
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal applicableAmount) {
        BigDecimal discount = switch (coupon.getDiscountType()) {
            case "AMOUNT" -> coupon.getDiscountValue();
            case "PERCENT" -> applicableAmount
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            default -> throw new IllegalArgumentException("不支援的優惠券折扣類型");
        };

        return discount.min(applicableAmount).max(BigDecimal.ZERO);
    }

    public record CouponItem(Product product, BigDecimal subtotal) {
    }

    public record AppliedCoupon(
            MemberCoupon memberCoupon,
            Coupon coupon,
            BigDecimal discount) {
    }
}
