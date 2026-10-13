package com.dinogo.review.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dinogo.review.dto.request.StarUpdateRequest;
import com.dinogo.review.dto.response.HistoryResponse;
import com.dinogo.review.dto.response.ProductRatingSummaryResponse;
import com.dinogo.review.dto.response.ProductReviewPageResponse;
import com.dinogo.review.dto.response.ReviewImageUploadResponse;
import com.dinogo.review.dto.response.SellerRatingSummaryResponse;
import com.dinogo.review.dto.response.StarResponse;
import com.dinogo.review.service.ReviewImageService;
import com.dinogo.review.service.ReviewService;
import com.dinogo.security.AuthenticatedMember;

import jakarta.validation.Valid;

/**
 * 評論 REST API。
 *
 * <p>
 * Controller 只負責接收 HTTP 資料、執行 @Valid、轉交 Service 及組合回應；
 * 會員身分透過 Spring Security principal 取得，不接受 query parameter 的 memberId。
 * </p>
 */
@Validated
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewImageService reviewImageService;

    public ReviewController(ReviewService reviewService, ReviewImageService reviewImageService) {
        this.reviewService = reviewService;
        this.reviewImageService = reviewImageService;
    }

    // review-start，總共1次修改，第1次//
    /** 功能：上傳至多三張評論圖；應用：回傳 URL 供 updateStar JSON 使用。 */
    @org.springframework.web.bind.annotation.PostMapping(value = "/stars/images", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewImageUploadResponse> uploadImages(
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal AuthenticatedMember member) {
        Integer memberId = requireMemberId(member);
        return ResponseEntity.ok(new ReviewImageUploadResponse(
                reviewImageService.upload(files, memberId)));
    }
    // review-end，總共1次修改，第1次//

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<HistoryResponse> getMemberHistory(
            @PathVariable Integer orderId,
            // Client：沿用會員模組的登入 principal，不再轉送 Authorization 給 Client。
            @AuthenticationPrincipal AuthenticatedMember member) {

        return ResponseEntity.ok(
                reviewService.getMemberHistory(orderId, requireMemberId(member)));
    }

    // 訂單清單與商品細項由 Order 模組提供；此端點只補上該訂單的評論資料。
    @GetMapping("/orders/{orderId}/stars")
    public ResponseEntity<List<StarResponse>> getMemberStars(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal AuthenticatedMember member) {

        return ResponseEntity.ok(
                reviewService.getMemberStars(orderId, requireMemberId(member)));
    }

    // 「新增評論」與「修改評論」共用：COMPLETED 時 Star 已預先建立。
    @PutMapping("/stars/{starId}")
    public ResponseEntity<StarResponse> updateStar(
            @PathVariable Integer starId,
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody StarUpdateRequest request) {

        return ResponseEntity.ok(
                reviewService.updateStar(starId, requireMemberId(member), request));
    }

    // 清除評論內容，但不刪除 Star 商品快照。
    @DeleteMapping("/stars/{starId}/content")
    public ResponseEntity<StarResponse> clearStar(
            @PathVariable Integer starId,
            @AuthenticationPrincipal AuthenticatedMember member) {

        return ResponseEntity.ok(
                reviewService.clearStar(starId, requireMemberId(member)));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductReviewPageResponse> getProductReviews(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "ALL") String content) {

        return ResponseEntity.ok(
                reviewService.getProductReviews(
                        productId,
                        page,
                        rating,
                        content));
    }

    /** product 模組的商品詳情頁使用；未有評分時 averageFiveStar 為 null。 */
    @GetMapping("/products/{productId}/rating-summary")
    public ResponseEntity<ProductRatingSummaryResponse> getProductRatingSummary(
            @PathVariable Integer productId) {

        return ResponseEntity.ok(reviewService.getProductRatingSummary(productId));
    }

    /** product／seller 模組使用，取得符合上架、售出及有評分條件的廠商評價。 */
    @GetMapping("/sellers/{sellerId}/rating-summary")
    public ResponseEntity<SellerRatingSummaryResponse> getSellerRatingSummary(
            @PathVariable Integer sellerId) {

        return ResponseEntity.ok(reviewService.getSellerRatingSummary(sellerId));
    }

    private Integer requireMemberId(AuthenticatedMember member) {
        if (member == null || member.memberId() == null) {
            throw new AuthenticationCredentialsNotFoundException("需要登入會員身分");
        }
        return member.memberId();
    }
}
