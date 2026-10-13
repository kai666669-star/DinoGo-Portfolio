package com.dinogo.seller.dto;

import java.util.List;

public record SellerSalesAiContent(
        String summary,
        String salesTrend,
        String productOpportunity,
        String couponOpportunity,
        String operationAlert,
        List<String> actions) {

    public SellerSalesAiContent {
        if (summary == null || summary.isBlank()) {
            summary = "目前資料仍有限，建議先持續累積訂單後再觀察銷售變化。";
        }
        if (salesTrend == null || salesTrend.isBlank()) {
            salesTrend = summary;
        }
        if (productOpportunity == null || productOpportunity.isBlank()) {
            productOpportunity = "優先確認熱賣商品庫存與商品資訊，避免有需求時無法出貨。";
        }
        if (couponOpportunity == null || couponOpportunity.isBlank()) {
            couponOpportunity = "可依目前銷售表現評估是否使用優惠券，提高買家下單意願。";
        }
        if (operationAlert == null || operationAlert.isBlank()) {
            operationAlert = "目前未發現明顯異常，建議持續觀察商品銷售集中度與出貨節奏。";
        }
        if (actions == null || actions.isEmpty()) {
            actions = List.of("優先確認熱賣商品庫存，避免有需求時無法出貨。");
        }
        if (actions.size() > 3) {
            actions = actions.subList(0, 3);
        }
    }
}
