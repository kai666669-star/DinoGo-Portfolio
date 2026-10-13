package com.dinogo.coupon.service;

import com.dinogo.coupon.dto.CouponCreateRequest;
import com.dinogo.coupon.dto.PublicCouponResponse;
import com.dinogo.coupon.dto.CouponResponse;
import com.dinogo.coupon.dto.CouponUpdateRequest;
import com.dinogo.coupon.entity.Coupon;
import com.dinogo.coupon.repository.CouponRepository;
import com.dinogo.coupon.repository.MemberCouponRepository;
import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.repository.ProductRepository;
import com.dinogo.seller.repository.SellerRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class CouponService {

    private static final Set<String> DISCOUNT_TYPES = Set.of("PERCENT", "AMOUNT");
    private static final Set<String> SCOPE_TYPES = Set.of("STORE", "ALL", "CATEGORY", "PRODUCT");
    private static final Set<String> PER_MEMBER_USAGE_POLICIES = Set.of("ONCE", "REPEAT");

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;

    public CouponService(
            CouponRepository couponRepository,
            MemberCouponRepository memberCouponRepository,
            SellerRepository sellerRepository,
            ProductRepository productRepository) {
        this.couponRepository = couponRepository;
        this.memberCouponRepository = memberCouponRepository;
        this.sellerRepository = sellerRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getCoupons(Integer sellerId) {
        return couponRepository.findBySellerIdOrderByCouponIdDesc(sellerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CouponResponse getCoupon(Integer sellerId, Integer couponId) {
        return toResponse(findSellerCoupon(sellerId, couponId));
    }

    @Transactional
    public CouponResponse createCoupon(Integer sellerId, CouponCreateRequest request) {
        validateTimeRange(request.startAt(), request.endAt());
        validateDiscountType(request.discountType());
        validateScopeType(request.scopeType(), request.categoryId(), request.productId());
        validatePerMemberUsagePolicy(resolvePerMemberUsagePolicy(request.perMemberUsagePolicy()));
        validateProductDiscount(sellerId, request.scopeType(), request.productId(),
                request.discountType(), request.discountValue());

        couponRepository.findBySellerIdAndCouponCode(sellerId, request.couponCode())
                .ifPresent(coupon -> {
                    throw new IllegalArgumentException("此賣家的優惠券代碼已存在");
                });

        Coupon coupon = new Coupon();
        coupon.setSellerId(sellerId);
        coupon.setCouponCode(request.couponCode());
        coupon.setCouponName(request.couponName());
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountValue(request.discountValue());
        coupon.setMinPurchaseAmount(request.minPurchaseAmount());
        coupon.setStartAt(request.startAt());
        coupon.setEndAt(request.endAt());
        coupon.setLimitCount(request.limitCount());
        coupon.setUsedCount(0);
        coupon.setPerMemberUsagePolicy(resolvePerMemberUsagePolicy(request.perMemberUsagePolicy()));
        coupon.setScopeType(request.scopeType());
        coupon.setCategoryId(request.categoryId());
        coupon.setProductId(request.productId());
        coupon.setStatus("DRAFT");

        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse updateCoupon(Integer sellerId, Integer couponId, CouponUpdateRequest request) {
        validateTimeRange(request.startAt(), request.endAt());
        validateDiscountType(request.discountType());
        validateScopeType(request.scopeType(), request.categoryId(), request.productId());
        validatePerMemberUsagePolicy(resolvePerMemberUsagePolicy(request.perMemberUsagePolicy()));
        validateProductDiscount(sellerId, request.scopeType(), request.productId(),
                request.discountType(), request.discountValue());

        Coupon coupon = findSellerCoupon(sellerId, couponId);
        coupon.setCouponName(request.couponName());
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountValue(request.discountValue());
        coupon.setMinPurchaseAmount(request.minPurchaseAmount());
        coupon.setStartAt(request.startAt());
        coupon.setEndAt(request.endAt());
        coupon.setLimitCount(request.limitCount());
        coupon.setPerMemberUsagePolicy(resolvePerMemberUsagePolicy(request.perMemberUsagePolicy()));
        coupon.setScopeType(request.scopeType());
        coupon.setCategoryId(request.categoryId());
        coupon.setProductId(request.productId());

        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse activateCoupon(Integer sellerId, Integer couponId) {
        Coupon coupon = findSellerCoupon(sellerId, couponId);
        coupon.setStatus("ACTIVE");
        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse disableCoupon(Integer sellerId, Integer couponId) {
        Coupon coupon = findSellerCoupon(sellerId, couponId);
        coupon.setStatus("DISABLED");
        return toResponse(couponRepository.save(coupon));
    }

    private Coupon findSellerCoupon(Integer sellerId, Integer couponId) {
        return couponRepository.findBySellerIdAndCouponId(sellerId, couponId)
                .orElseThrow(() -> new IllegalArgumentException("找不到指定的優惠券"));
    }

    private CouponResponse toResponse(Coupon coupon) {
        String sellerName = sellerRepository.findById(coupon.getSellerId())
                .map(seller -> seller.getStoreName())
                .orElse("未知賣家");
        return CouponResponse.from(coupon, sellerName);
    }

    private PublicCouponResponse toPublicResponse(Coupon coupon) {
        String sellerName = sellerRepository.findById(coupon.getSellerId())
                .map(seller -> seller.getStoreName())
                .orElse("未知賣家");
        return PublicCouponResponse.from(coupon, sellerName);
    }

    private void validateTimeRange(java.time.LocalDateTime startAt, java.time.LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("開始與結束時間不可為空");
        }
        if (startAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("開始時間不可早於目前時間");
        }
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("結束時間必須在開始時間之後");
        }
    }

    private void validateProductDiscount(
            Integer sellerId,
            String scopeType,
            Integer productId,
            String discountType,
            java.math.BigDecimal discountValue) {
        if (!"PRODUCT".equals(scopeType) || productId == null || !"AMOUNT".equals(discountType)) {
            return;
        }

        Product product = productRepository.findBySeller_SellerIdAndProductId(sellerId, productId)
                .orElseThrow(() -> new IllegalArgumentException("找不到此賣家的適用商品"));

        if (discountValue.compareTo(product.getBasePrice()) >= 0) {
            throw new IllegalArgumentException("固定折扣金額必須小於商品價格");
        }
    }

    private void validateDiscountType(String discountType) {
        if (!DISCOUNT_TYPES.contains(discountType)) {
            throw new IllegalArgumentException("無效的折扣類型，請選擇 PERCENT 或 AMOUNT");
        }
    }

    private String resolvePerMemberUsagePolicy(String perMemberUsagePolicy) {
        if (perMemberUsagePolicy == null || perMemberUsagePolicy.isBlank()) {
            return "ONCE";
        }
        return perMemberUsagePolicy;
    }

    private void validatePerMemberUsagePolicy(String perMemberUsagePolicy) {
        if (!PER_MEMBER_USAGE_POLICIES.contains(perMemberUsagePolicy)) {
            throw new IllegalArgumentException("無效的會員使用次數設定，請選擇 ONCE 或 REPEAT");
        }
    }

    private void validateScopeType(String scopeType, Integer categoryId, Integer productId) {
        if (!SCOPE_TYPES.contains(scopeType)) {
            throw new IllegalArgumentException("無效的範圍類型，請選擇 STORE、ALL、CATEGORY 或 PRODUCT");
        }
        if ("CATEGORY".equals(scopeType) && categoryId == null) {
            throw new IllegalArgumentException("適用分類優惠券必須提供 categoryId");
        }
        if ("PRODUCT".equals(scopeType) && productId == null) {
            throw new IllegalArgumentException("適用商品優惠券必須提供 productId");
        }
    }

    @Transactional(readOnly = true)
    public List<PublicCouponResponse> getAvailableCoupons(Integer sellerId) {
        return getAvailableCoupons(sellerId, null);
    }

    @Transactional(readOnly = true)
    public List<PublicCouponResponse> getAvailableCoupons(
            Integer sellerId,
            Integer productId) {

        LocalDateTime now = LocalDateTime.now();

        return couponRepository.findAllByOrderByCouponIdDesc()
                .stream()
                .peek(coupon -> {
                    if (Integer.valueOf(2014).equals(coupon.getCouponId())) {
                        long claimedCount = memberCouponRepository.countByCouponId(coupon.getCouponId());

                    }
                })

                // 排除已停用的優惠券
                .filter(coupon -> !"DISABLED".equals(coupon.getStatus()))

                // 有指定 sellerId 時，只查該賣家的優惠券
                .filter(coupon -> sellerId == null
                        || sellerId.equals(coupon.getSellerId()))

                // 已經到達開始時間
                .filter(coupon -> !coupon.getStartAt().isAfter(now))

                // 尚未超過結束時間
                .filter(coupon -> !coupon.getEndAt().isBefore(now))

                // 沒有限量，或者尚未被領完
                .filter(coupon -> coupon.getLimitCount() == null
                        || memberCouponRepository.countByCouponId(coupon.getCouponId()) < coupon.getLimitCount())

                /*
                 * 沒有 productId：
                 * 保留原本行為，回傳該賣家的全部可用優惠券。
                 *
                 * 有 productId：
                 * 只回傳全店適用或目前商品適用的優惠券。
                 */
                .filter(coupon -> productId == null
                        || "STORE".equals(coupon.getScopeType())
                        || "ALL".equals(coupon.getScopeType())
                        || ("PRODUCT".equals(coupon.getScopeType())
                                && productId.equals(coupon.getProductId())))

                .map(this::toPublicResponse)
                .toList();
    }
}
