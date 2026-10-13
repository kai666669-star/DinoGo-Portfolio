package com.dinogo.seller.dto;

import java.time.LocalTime;

import com.dinogo.seller.entity.Seller;

public record SellerProfileResponse(
        Integer sellerId,
        Integer memberId,
        String storeName,
        String storeDescription,
        String storeLogoUrl,
        String status,
        LocalTime serviceStartTime,
        LocalTime serviceEndTime) {

    public static SellerProfileResponse from(Seller seller) {
        return new SellerProfileResponse(
                seller.getSellerId(),
                seller.getMemberId(),
                seller.getStoreName(),
                seller.getStoreDescription(),
                seller.getStoreLogoUrl(),
                seller.getStatus(),
                seller.getServiceStartTime(),
                seller.getServiceEndTime());
    }
}