package com.dinogo.review.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.member.dto.MemberSysmsgResponse;
import com.dinogo.review.dto.request.StarUpdateRequest;
import com.dinogo.review.dto.response.HistoryResponse;
import com.dinogo.review.dto.response.ProductRatingSummaryResponse;
import com.dinogo.review.dto.response.ProductReviewPageResponse;
import com.dinogo.review.dto.response.ProductReviewResponse;
import com.dinogo.review.dto.response.ProductReviewSummaryResponse;
import com.dinogo.review.dto.response.SellerRatingSummaryResponse;
import com.dinogo.review.dto.response.StarResponse;
import com.dinogo.review.entity.HistoryEntity;
import com.dinogo.review.entity.StarEntity;
import com.dinogo.review.exception.ReviewConflictException;
import com.dinogo.review.exception.ReviewForbiddenException;
import com.dinogo.review.exception.ReviewNotFoundException;
import com.dinogo.review.repository.HistoryRepository;
import com.dinogo.review.repository.ProductReviewAggregate;
import com.dinogo.review.repository.SellerRatingAggregate;
import com.dinogo.review.repository.StarRepository;
//review-start，總共9次修改，第1次//
import com.dinogo.salesii.dto.OrderReviewItemResponse;
import com.dinogo.salesii.dto.OrderSysmsgResponse;
//review-end，總共9次修改，第1次//

/** 評論建立、修改、清除、查詢與產品評論分頁的主要業務層。 */
@Service
public class ReviewService {

    private static final int PAGE_SIZE = 10;
    private static final Sort PRODUCT_REVIEW_SORT = Sort.by(
            Sort.Order.desc("reviewPriority"),
            Sort.Order.desc("starUpdAt"),
            Sort.Order.desc("id"));
    private final HistoryRepository historyRepository;
    private final StarRepository starRepository;
    private final MonolithValidationService monolithValidationService;
    // review-start，總共9次修改，第2次//
    private final ReviewImageService reviewImages;

    public ReviewService(
            HistoryRepository historyRepository,
            StarRepository starRepository,
            MonolithValidationService monolithValidationService,
            ReviewImageService reviewImages) {
        this.historyRepository = historyRepository;
        this.starRepository = starRepository;
        this.monolithValidationService = monolithValidationService;
        this.reviewImages = reviewImages;
    }
    // review-end，總共9次修改，第2次//

    @Transactional
    // review-start，總共9次修改，第3次//
    public HistoryResponse createHistoryFromCompletedOrder(OrderSysmsgResponse orderSnapshot) {
        OrderSysmsgResponse order = monolithValidationService.requireCompletedOrder(orderSnapshot);
        Integer orderId = order.orderId();
        // review-end，總共9次修改，第3次//
        if (historyRepository.findByOrderId(orderId).isPresent()) {
            throw new ReviewConflictException("此訂單已建立評論紀錄，orderId=" + orderId);
        }
        if (order.items().isEmpty()) {
            throw new IllegalStateException("訂單 Service 未回傳商品明細");
        }

        HistoryEntity history = new HistoryEntity();
        history.setMemberId(order.buyerId());
        history.setSellerId(order.sellerId());
        history.setOrderId(order.orderId());
        // review-start，總共9次修改，第4次//
        // 保存訂單編號快照，對應 review.history.order_no NOT NULL。
        history.setOrderNo(order.orderNo());
        // review-end，總共9次修改，第4次//
        for (OrderReviewItemResponse item : order.items()) {
            history.addStar(toStarSnapshot(item));
        }

        try {
            HistoryEntity savedHistory = historyRepository.saveAndFlush(history);
            return HistoryResponse.fromEntity(savedHistory, savedHistory.getStars());
        } catch (DataIntegrityViolationException exception) {
            throw new ReviewConflictException(
                    "訂單評論已由另一個請求建立，orderId=" + orderId,
                    exception);
        }
    }

    @Transactional
    // review-start，總共9次修改，第5次//
    public void deleteHistoryForCancelledOrder(OrderSysmsgResponse orderSnapshot) {
        OrderSysmsgResponse order = monolithValidationService.requireCancelledOrder(orderSnapshot);
        Integer orderId = order.orderId();
        historyRepository.findByOrderId(orderId).ifPresent(historyRepository::delete);
    }
    // review-end，總共9次修改，第5次//

    @Transactional(readOnly = true)
    public HistoryResponse getMemberHistory(Integer orderId, Integer currentMemberId) {
        MemberSysmsgResponse member = monolithValidationService.requireCurrentMember(currentMemberId);
        HistoryEntity history = requireHistory(orderId);
        monolithValidationService.requireOwnership(history.getMemberId(), member.memberId());
        List<StarEntity> stars = starRepository.findByHistoryIdOrderByOrderItemIdAsc(history.getId());
        return HistoryResponse.fromEntity(history, stars);
    }

    @Transactional(readOnly = true)
    public List<StarResponse> getMemberStars(Integer orderId, Integer currentMemberId) {
        MemberSysmsgResponse member = monolithValidationService.requireCurrentMember(currentMemberId);
        HistoryEntity history = requireHistory(orderId);
        monolithValidationService.requireOwnership(history.getMemberId(), member.memberId());
        return starRepository.findByHistoryIdOrderByOrderItemIdAsc(history.getId()).stream()
                .map(StarResponse::fromEntity)
                .toList();
    }

    @Transactional
    public StarResponse updateStar(Integer starId, Integer currentMemberId, StarUpdateRequest request) {
        MemberSysmsgResponse member = monolithValidationService.requireCurrentMember(currentMemberId);
        validateImageReferences(request, member.memberId());
        StarEntity star = requireOwnedStar(starId, member.memberId());
        star.setFiveStar(request.fiveStar());
        star.setFeedback(normalizeFeedback(request.feedback()));
        star.setImgOne(request.imgOne());
        star.setImgOnePublicId(request.imgOnePublicId());
        star.setImgTwo(request.imgTwo());
        star.setImgTwoPublicId(request.imgTwoPublicId());
        star.setImgThree(request.imgThree());
        star.setImgThreePublicId(request.imgThreePublicId());
        star.setStarUpdAt(LocalDateTime.now());
        return StarResponse.fromEntity(starRepository.saveAndFlush(star));
    }

    @Transactional
    public StarResponse clearStar(Integer starId, Integer currentMemberId) {
        MemberSysmsgResponse member = monolithValidationService.requireCurrentMember(currentMemberId);
        StarEntity star = requireOwnedStar(starId, member.memberId());
        star.setFiveStar(null);
        star.setFeedback(null);
        star.setImgOne(null);
        star.setImgOnePublicId(null);
        star.setImgTwo(null);
        star.setImgTwoPublicId(null);
        star.setImgThree(null);
        star.setImgThreePublicId(null);
        star.setStarUpdAt(LocalDateTime.now());
        return StarResponse.fromEntity(starRepository.saveAndFlush(star));
    }

    @Transactional(readOnly = true)
    public ProductReviewPageResponse getProductReviews(
            Integer productId,
            Integer pageNumber,
            Integer rating,
            String content) {

        requirePositiveId(productId, "productId");

        if (pageNumber == null || pageNumber < 1) {
            throw new IllegalArgumentException("page 必須大於等於 1");
        }

        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("rating 必須介於 1 到 5");
        }

        String contentFilter = normalizeContentFilter(content);

        PageRequest pageable = PageRequest.of(
                pageNumber - 1,
                PAGE_SIZE,
                PRODUCT_REVIEW_SORT);

        Page<StarEntity> page = starRepository.findPublicProductReviews(
                productId,
                rating,
                contentFilter,
                pageable);

        if (pageNumber > 1
                && page.isEmpty()
                && page.getTotalElements() > 0) {

            throw new IllegalArgumentException("page 超出最後一頁");
        }

        List<ProductReviewResponse> contentList = page.getContent()
                .stream()
                .map(ProductReviewResponse::fromEntity)
                .toList();

        return new ProductReviewPageResponse(
                contentList,
                page.hasNext(),
                productReviewSummary(productId),
                pageNumber,
                page.getTotalPages(),
                page.getTotalElements());
    }

    private String normalizeContentFilter(String contentFilter) {
        if (contentFilter == null || contentFilter.isBlank()) {
            return "ALL";
        }
        String normalized = contentFilter.trim().toUpperCase(java.util.Locale.ROOT);
        if (!List.of("ALL", "FEEDBACK", "IMAGE").contains(normalized)) {
            throw new IllegalArgumentException("content 必須是 ALL、FEEDBACK 或 IMAGE");
        }
        return normalized;
    }

    /** 功能：映射單次聚合結果；應用：產品摘要固定只執行一支聚合 SQL。 */
    private ProductReviewSummaryResponse productReviewSummary(Integer productId) {
        ProductReviewAggregate aggregate = starRepository.aggregateProductReviews(productId);
        return new ProductReviewSummaryResponse(
                truncateToOneDecimal(aggregate.getAverageFiveStar()),
                aggregate.getTotalCount(),
                aggregate.getFiveStarCount(),
                aggregate.getFourStarCount(),
                aggregate.getThreeStarCount(),
                aggregate.getTwoStarCount(),
                aggregate.getOneStarCount(),
                aggregate.getWithFeedbackCount(),
                aggregate.getWithImageCount());
    }

    @Transactional(readOnly = true)
    public ProductRatingSummaryResponse getProductRatingSummary(Integer productId) {
        requirePositiveId(productId, "productId");
        ProductReviewAggregate aggregate = starRepository.aggregateProductReviews(productId);
        return new ProductRatingSummaryResponse(
                productId,
                truncateToOneDecimal(aggregate.getAverageFiveStar()),
                aggregate.getTotalCount());
    }

    /**
     * 作為廠商整體評價；每一筆符合條件的 five_star 權重相同，結果無條件捨去至小數一位。
     */
    @Transactional(readOnly = true)
    public SellerRatingSummaryResponse getSellerRatingSummary(Integer sellerId) {
        requirePositiveId(sellerId, "sellerId");
        SellerRatingAggregate aggregate = starRepository.aggregateSellerRatings(sellerId);
        return new SellerRatingSummaryResponse(
                sellerId,
                truncateToOneDecimal(aggregate.getAverageFiveStar()),
                aggregate.getRatingCount(),
                aggregate.getRatedProductCount());
    }

    private BigDecimal truncateToOneDecimal(BigDecimal average) {
        return average == null
                ? null
                : average.setScale(1, RoundingMode.DOWN);
    }

    private HistoryEntity requireHistory(Integer orderId) {
        requirePositiveId(orderId, "orderId");
        return historyRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ReviewNotFoundException(
                        "找不到此訂單的評論紀錄，orderId=" + orderId));
    }

    private StarEntity requireOwnedStar(Integer starId, Integer memberId) {
        requirePositiveId(starId, "starId");
        // review-start，總共9次修改，第7次//
        // 先區分不存在與非本人資源，才能依架構分別回傳 404 與 403。
        StarEntity star = starRepository.findById(starId)
                .orElseThrow(() -> new ReviewNotFoundException(
                        "找不到評論，starId=" + starId));
        if (!memberId.equals(star.getHistory().getMemberId())) {
            throw new ReviewForbiddenException("目前會員無權操作此評論資料");
        }
        return star;
        // review-end，總共9次修改，第7次//
    }

    private StarEntity toStarSnapshot(OrderReviewItemResponse item) {
        if (item == null || item.orderItemId() == null || item.productId() == null
                || item.productName() == null || item.unitPrice() == null) {
            throw new IllegalStateException("訂單商品明細缺少評論快照必要欄位");
        }
        // review-start，總共9次修改，第8次//
        // 上限對齊實際 OrderItem 欄位，合法訂單快照不應因 review 欄位較短而失敗。
        if (item.productName().length() > 100) {
            throw new IllegalStateException("訂單商品名稱超過 review.star.product_name 長度 100");
        }
        if (item.productImageUrl() != null && item.productImageUrl().length() > 500) {
            throw new IllegalStateException("訂單商品圖片 URL 超過長度 500");
        }
        // image_url 允許 NULL，代表完成訂單建立快照時商品沒有可用圖片。
        // review-end，總共9次修改，第8次//
        if (item.unitPrice().signum() < 0) {
            throw new IllegalStateException("訂單商品價格不可小於 0");
        }

        StarEntity star = new StarEntity();
        star.setOrderItemId(item.orderItemId());
        star.setProductId(item.productId());
        star.setProductName(item.productName());
        star.setImageUrl(item.productImageUrl());
        star.setBasePrice(item.unitPrice());
        return star;
    }

    private void validateImageReferences(StarUpdateRequest request, Integer memberId) {
        // review-start，總共9次修改，第9次//
        // URL 與 publicId 成對驗證 cloud name 及會員 prefix；不接受任意 Cloudinary URL。
        String ownerPrefix = "dinogo/reviews/" + memberId;
        reviewImages.validateReference(request.imgOne(), request.imgOnePublicId(), ownerPrefix);
        reviewImages.validateReference(request.imgTwo(), request.imgTwoPublicId(), ownerPrefix);
        reviewImages.validateReference(request.imgThree(), request.imgThreePublicId(), ownerPrefix);
        // review-end，總共9次修改，第9次//
    }

    private String normalizeFeedback(String feedback) {
        if (feedback == null) {
            return null;
        }
        String normalized = feedback.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void requirePositiveId(Integer id, String name) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(name + " 必須是正整數");
        }
    }
}
