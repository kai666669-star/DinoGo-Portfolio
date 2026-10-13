package com.dinogo.cart.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.cart.dto.CheckoutPreviewItemRequest;
import com.dinogo.cart.dto.CheckoutPreviewRequest;
import com.dinogo.cart.dto.CheckoutPreviewResponse;
import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductSku;
import com.dinogo.catalog.repository.ProductSkuRepository;
import com.dinogo.coupon.service.CouponUsageService;
import com.dinogo.coupon.service.CouponUsageService.AppliedCoupon;
import com.dinogo.coupon.service.CouponUsageService.CouponItem;
import com.dinogo.member.entity.Address;
import com.dinogo.member.repository.AddressRepository;

@Service
@Transactional(readOnly = true)
public class CheckoutService {

        private static final String HOME_DELIVERY = "HOME_DELIVERY";

        private final ProductSkuRepository productSkuRepository;
        private final AddressRepository addressRepository;
        private final CouponUsageService couponUsageService;

        public CheckoutService(
                        ProductSkuRepository productSkuRepository,
                        AddressRepository addressRepository,
                        CouponUsageService couponUsageService) {

                this.productSkuRepository = productSkuRepository;
                this.addressRepository = addressRepository;
                this.couponUsageService = couponUsageService;
        }

        // =========================================================
        // 結帳預覽
        // =========================================================

        public CheckoutPreviewResponse preview(
                        CheckoutPreviewRequest request,
                        Integer memberId) {

                if (!HOME_DELIVERY.equals(request.shippingMethod())) {
                        throw new IllegalArgumentException("目前僅支援宅配");
                }

                // =====================================================
                // 1. 驗證地址
                // =====================================================

                Address address = addressRepository
                                .findById(request.addressId())
                                .orElseThrow(() -> new RuntimeException(
                                                "收件地址不存在"));

                // 確認地址屬於目前會員
                if (!address.getMember()
                                .getMemberId()
                                .equals(memberId)) {

                        throw new RuntimeException(
                                        "無權使用此收件地址");
                }

                // =====================================================
                // 2. 防止重複 SKU
                // =====================================================

                Set<Integer> skuIds = new HashSet<>();

                BigDecimal subtotal = BigDecimal.ZERO;
                List<CouponItem> couponItems = new ArrayList<>();

                // 用來確認這次結帳是哪一個賣家
                Integer checkoutSellerId = null;

                // =====================================================
                // 3. 檢查每個商品
                // =====================================================

                for (CheckoutPreviewItemRequest item : request.items()) {

                        // -------------------------------------------------
                        // 防止同一 SKU 重複
                        // -------------------------------------------------

                        if (!skuIds.add(item.skuId())) {

                                throw new RuntimeException(
                                                "結帳商品不可有重複 SKU："
                                                                + item.skuId());
                        }

                        // -------------------------------------------------
                        // 數量檢查
                        // -------------------------------------------------

                        if (item.quantity() == null
                                        || item.quantity() <= 0) {

                                throw new RuntimeException(
                                                "商品數量必須大於 0，SKU："
                                                                + item.skuId());
                        }

                        // -------------------------------------------------
                        // 查 SKU
                        // -------------------------------------------------

                        ProductSku sku = productSkuRepository
                                        .findById(item.skuId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "SKU 不存在："
                                                                        + item.skuId()));

                        // -------------------------------------------------
                        // SKU 是否啟用
                        // -------------------------------------------------

                        if (!Byte.valueOf((byte) 1)
                                        .equals(sku.getStatus())) {

                                throw new RuntimeException(
                                                "商品規格已停用，SKU："
                                                                + item.skuId());
                        }

                        // -------------------------------------------------
                        // Product 是否上架
                        // -------------------------------------------------

                        Product product = sku.getProduct();

                        if (!Byte.valueOf((byte) 1)
                                        .equals(product.getStatus())) {

                                throw new RuntimeException(
                                                "商品已下架："
                                                                + product.getProductName());
                        }

                        // -------------------------------------------------
                        // 取得賣家
                        // -------------------------------------------------

                        if (product.getSeller() == null) {

                                throw new RuntimeException(
                                                "商品沒有設定賣家："
                                                                + product.getProductName());
                        }

                        Integer itemSellerId = product.getSeller().getSellerId();

                        // -------------------------------------------------
                        // 確認所有商品都是同一個賣家
                        // -------------------------------------------------

                        if (checkoutSellerId == null) {

                                checkoutSellerId = itemSellerId;

                        } else if (!checkoutSellerId.equals(itemSellerId)) {

                                throw new RuntimeException(
                                                "不同賣家的商品不能一起結帳");
                        }

                        // -------------------------------------------------
                        // 庫存
                        // -------------------------------------------------

                        if (sku.getStock() == null
                                        || item.quantity() > sku.getStock()) {

                                throw new RuntimeException(
                                                product.getProductName()
                                                                + " 庫存不足，目前剩餘 "
                                                                + sku.getStock()
                                                                + " 件");
                        }

                        // -------------------------------------------------
                        // 使用 DB 真實價格
                        // -------------------------------------------------

                        BigDecimal itemSubtotal = sku.getPrice()
                                        .setScale(0, RoundingMode.HALF_UP)
                                        .multiply(
                                                        BigDecimal.valueOf(item.quantity()));

                        subtotal = subtotal.add(itemSubtotal);
                        couponItems.add(new CouponItem(product, itemSubtotal));
                }

                // =====================================================
                // 4. 運費
                // =====================================================

                BigDecimal shippingFee = BigDecimal.ZERO;

                // =====================================================
                // 5. 優惠券折扣
                // =====================================================

                BigDecimal discount = BigDecimal.ZERO;
                if (request.memberCouponId() != null) {

                        AppliedCoupon appliedCoupon = couponUsageService.validateAndCalculate(
                                        request.memberCouponId(),
                                        memberId,
                                        checkoutSellerId,
                                        subtotal,
                                        couponItems);
                        discount = appliedCoupon.discount();
                }

                // =====================================================
                // 6. 總額
                // =====================================================

                BigDecimal totalAmount = subtotal
                                .add(shippingFee)
                                .subtract(discount);

                // 防止折扣超過訂單金額
                if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
                        totalAmount = BigDecimal.ZERO;
                }

                // =====================================================
                // 7. 回傳
                // =====================================================

                return new CheckoutPreviewResponse(
                                subtotal.setScale(0, RoundingMode.HALF_UP),
                                shippingFee.setScale(0, RoundingMode.HALF_UP),
                                discount.setScale(0, RoundingMode.HALF_UP),
                                totalAmount.setScale(0, RoundingMode.HALF_UP));
        }
}
