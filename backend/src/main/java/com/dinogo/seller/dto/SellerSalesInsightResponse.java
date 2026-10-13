package com.dinogo.seller.dto;

import java.time.LocalDateTime;

public record SellerSalesInsightResponse(
        SellerSalesInsightStats stats,
        SellerSalesAiContent ai,
        boolean generatedByAi,
        String modelName,
        LocalDateTime generatedAt) {
}
