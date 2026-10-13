package com.dinogo.seller.dto;

import java.time.LocalDateTime;

import com.dinogo.seller.entity.SellerApplication;
import com.dinogo.seller.entity.SellerApplicationStatus;

public record SellerApplicationResponse(
        Integer applicationId,
        Integer memberId,
        String storeName,
        String storeDescription,
        String storeLogoUrl,
        SellerApplicationStatus status,
        String rejectReason,
        Integer reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static SellerApplicationResponse from(SellerApplication application) {
        return new SellerApplicationResponse(
                application.getApplicationId(),
                application.getMemberId(),
                application.getStoreName(),
                application.getStoreDescription(),
                application.getStoreLogoUrl(),
                application.getStatus(),
                application.getRejectReason(),
                application.getReviewedBy(),
                application.getReviewedAt(),
                application.getCreatedAt(),
                application.getUpdatedAt());
    }
}
