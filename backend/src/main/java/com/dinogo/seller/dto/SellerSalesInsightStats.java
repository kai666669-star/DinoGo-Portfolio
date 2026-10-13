package com.dinogo.seller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SellerSalesInsightStats(
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate previousPeriodStart,
        LocalDate previousPeriodEnd,
        BigDecimal revenueAmount,
        Integer orderCount,
        BigDecimal averageOrderValue,
        Integer soldQuantity,
        BigDecimal previousRevenueAmount,
        Integer previousOrderCount,
        BigDecimal previousAverageOrderValue,
        Integer previousSoldQuantity,
        BigDecimal revenueChangeRate,
        BigDecimal orderCountChangeRate,
        BigDecimal averageOrderValueChangeRate,
        List<ProductSalesSummary> topProducts,
        List<ProductSalesSummary> lowProducts) {

    public record ProductSalesSummary(
            Integer productId,
            String productName,
            BigDecimal revenueAmount,
            Integer soldQuantity,
            Integer orderCount) {
    }
}
