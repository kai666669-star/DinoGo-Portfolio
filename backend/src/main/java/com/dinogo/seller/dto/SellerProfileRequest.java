package com.dinogo.seller.dto;

import java.time.LocalTime;

public record SellerProfileRequest(
        String storeName,
        String storeDescription,
        String storeLogoUrl,
        String status,
        LocalTime serviceStartTime,
        LocalTime serviceEndTime) {
}