package com.dinogo.salesii.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

//rev+msg-start，總共1次修改，第1次//
/**
 * 功能：提供 Review 建立或刪除評論主檔所需的訂單快照。
 * 應用：訂單狀態進入 COMPLETED／CANCELLED 時，由訂單模組直接傳給 ReviewService。
 */
public record OrderSysmsgResponse(
        Integer orderId,
        String orderNo,
        Integer buyerId,
        Integer sellerId,
        String status,
        List<OrderReviewItemResponse> items,
        BigDecimal totalAmount,
        Integer paymentMethodId,
        String methodName,
        String methodCode,
        LocalDateTime createdAt,
        String cancelReason,
        LocalDateTime cancelledAt,
        String orderStatus,
        String paymentStatus,
        LocalDateTime paidAt,
        String shipmentStatus,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        LocalDateTime completedAt) {

    public OrderSysmsgResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /** 相容既有 Review／Sysmsg 測試；未提供的生命週期快照保持 null。 */
    public OrderSysmsgResponse(
            Integer orderId,
            String orderNo,
            Integer buyerId,
            Integer sellerId,
            String status,
            List<OrderReviewItemResponse> items,
            BigDecimal totalAmount,
            Integer paymentMethodId,
            String methodName,
            LocalDateTime createdAt,
            String cancelReason,
            LocalDateTime cancelledAt) {
        this(orderId, orderNo, buyerId, sellerId, status, items,
                totalAmount, paymentMethodId, methodName, null, createdAt, cancelReason, cancelledAt,
                status, null, null, null, null, null, null);
    }

    public OrderSysmsgResponse(
            Integer orderId,
            String orderNo,
            Integer buyerId,
            Integer sellerId,
            String status,
            List<OrderReviewItemResponse> items,
            BigDecimal totalAmount,
            Integer paymentMethodId,
            String methodName,
            String methodCode,
            LocalDateTime createdAt,
            String cancelReason,
            LocalDateTime cancelledAt) {
        this(orderId, orderNo, buyerId, sellerId, status, items,
                totalAmount, paymentMethodId, methodName, methodCode, createdAt, cancelReason, cancelledAt,
                status, null, null, null, null, null, null);
    }

    /** Review 測試或只需狀態快照時使用；訊息專用欄位保持 null。 */
    public OrderSysmsgResponse(
            Integer orderId,
            String orderNo,
            Integer buyerId,
            Integer sellerId,
            String status,
            List<OrderReviewItemResponse> items) {
        this(orderId, orderNo, buyerId, sellerId, status, items,
                null, null, null, null, null, null, null,
                status, null, null, null, null, null, null);
    }
}
//rev+msg-end，總共1次修改，第1次//
