package com.dinogo.seller.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dinogo.catalog.repository.ProductRepository;
import com.dinogo.coupon.service.CouponService;
//review-start，總共4次修改，第1次//
import com.dinogo.review.dto.response.SellerRatingSummaryResponse;
import com.dinogo.review.service.ReviewService;
//review-end，總共4次修改，第1次//
import com.dinogo.seller.dto.SellerProfileRequest;
import com.dinogo.seller.dto.SellerProfileResponse;
import com.dinogo.seller.dto.StorefrontSummaryResponse;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

@Service
public class SellerProfileService {

    private static final long MAX_LOGO_SIZE_BYTES = 5 * 1024 * 1024;

    private final SellerRepository sellerRepository;
    private final Cloudinary cloudinary;
    private final ProductRepository productRepository;
    //review-start，總共4次修改，第2次//
    // 功能：seller 只呼叫 Review 公開服務，不直接依賴 Review Repository。
    // 應用：Review 可自行調整聚合查詢，seller 不受資料存取方法變更影響。
    private final ReviewService reviewService;
    //review-end，總共4次修改，第2次//
    private final CouponService couponService;

    public SellerProfileService(
            SellerRepository sellerRepository,
            Cloudinary cloudinary,
            ProductRepository productRepository,
            //review-start，總共4次修改，第3次//
            ReviewService reviewService,
            //review-end，總共4次修改，第3次//
            CouponService couponService) {
        this.sellerRepository = sellerRepository;
        this.cloudinary = cloudinary;
        this.productRepository = productRepository;
        this.reviewService = reviewService;
        this.couponService = couponService;
    }

    @Transactional(readOnly = true)
    public SellerProfileResponse getMyProfile(Integer memberId) {
        Seller seller = sellerRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found."));
        return SellerProfileResponse.from(seller);
    }

    @Transactional
    public SellerProfileResponse updateMyProfile(Integer memberId, SellerProfileRequest request) {
        Seller seller = sellerRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found."));

        seller.setStoreName(request.storeName());
        seller.setStoreDescription(request.storeDescription());
        seller.setStoreLogoUrl(SellerCloudinaryUrlValidator.optionalCloudinaryImageUrl(
                request.storeLogoUrl(), "店鋪 Logo URL"));
        seller.setStatus(request.status());
        seller.setServiceStartTime(request.serviceStartTime());
        seller.setServiceEndTime(request.serviceEndTime());
        seller.setUpdatedAt(LocalDateTime.now());
        return SellerProfileResponse.from(sellerRepository.save(seller));
    }

    @Transactional(readOnly = true)
    public SellerProfileResponse getPublicStore(Integer sellerId) {
        Seller seller = sellerRepository.findBySellerIdAndStatusIgnoreCase(sellerId, "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("Store not found."));
        return SellerProfileResponse.from(seller);
    }

    @Transactional(readOnly = true)
    public StorefrontSummaryResponse getStorefrontSummary(Integer sellerId) {
        sellerRepository.findBySellerIdAndStatusIgnoreCase(sellerId, "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("Store not found."));

        //review-start，總共4次修改，第4次//
        // 功能：由 ReviewService 一次聚合平均、評分數與已評商品數。
        // 應用：平均值統一由 Review 以 RoundingMode.DOWN 無條件捨去至小數一位。
        SellerRatingSummaryResponse rating = reviewService.getSellerRatingSummary(sellerId);

        return new StorefrontSummaryResponse(
                rating.averageFiveStar(),
                rating.ratingCount(),
                productRepository.countBySeller_SellerIdAndStatus(sellerId, (byte) 1),
                productRepository.sumSoldCountBySellerId(sellerId),
                couponService.getAvailableCoupons(sellerId).size());
        //review-end，總共4次修改，第4次//
    }

    @Transactional(readOnly = true)
    public List<SellerProfileResponse> searchPublicStores(String keyword) {
        String safeKeyword = keyword == null ? "" : keyword.trim();
        return sellerRepository
                .findByStoreNameContainingAndStatusIgnoreCase(safeKeyword, "ACTIVE")
                .stream()
                .map(SellerProfileResponse::from)
                .toList();
    }

    @Transactional
    public SellerProfileResponse uploadLogo(Integer memberId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Logo file is required.");
        }
        if (file.getSize() > MAX_LOGO_SIZE_BYTES) {
            throw new IllegalArgumentException("Logo file must be 5 MB or smaller.");
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        boolean isImageContentType = contentType != null && contentType.startsWith("image/");
        boolean isImageExtension = originalFilename != null
                && originalFilename.toLowerCase().matches(".*\\.(jpg|jpeg|png|webp|gif)$");
        if (!isImageContentType || !isImageExtension) {
            throw new IllegalArgumentException("Only image files are allowed.");
        }

        Seller seller = sellerRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found."));
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", "dinogo/seller-logos", "resource_type", "image"));
            seller.setStoreLogoUrl(uploadResult.get("secure_url").toString());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to upload seller logo.", exception);
        }

        seller.setUpdatedAt(LocalDateTime.now());
        return SellerProfileResponse.from(sellerRepository.save(seller));
    }
}
