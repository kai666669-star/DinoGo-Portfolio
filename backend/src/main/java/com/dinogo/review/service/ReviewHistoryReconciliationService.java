package com.dinogo.review.service;

import org.springframework.stereotype.Service;

import com.dinogo.review.exception.ReviewConflictException;
import com.dinogo.review.repository.HistoryRepository;
import com.dinogo.salesii.dto.OrderSysmsgResponse;

//review-start，總共1次修改，第1次//
/**
 * 功能：依權威訂單快照補齊完成訂單的 History，並清除取消訂單的 History。
 * 應用：事件處理與每分鐘自檢共用；已存在的 History 視為成功，不重建也不覆寫評價。
 */
@Service
public class ReviewHistoryReconciliationService {
    private final HistoryRepository histories;
    private final ReviewService reviews;

    public ReviewHistoryReconciliationService(
            HistoryRepository histories,
            ReviewService reviews) {
        this.histories = histories;
        this.reviews = reviews;
    }

    public void reconcile(OrderSysmsgResponse order) {
        if (order == null || order.orderId() == null || order.status() == null) {
            throw new IllegalArgumentException("訂單快照缺少自檢必要欄位");
        }
        String rawStatus = order.orderStatus() == null ? order.status() : order.orderStatus();
        String status = rawStatus.trim().toUpperCase(java.util.Locale.ROOT);
        if ("COMPLETED".equals(status)) {
            createIfMissing(order);
        } else if ("CANCELLED".equals(status)) {
            reviews.deleteHistoryForCancelledOrder(order);
        }
    }

    private void createIfMissing(OrderSysmsgResponse order) {
        if (histories.findByOrderId(order.orderId()).isPresent()) {
            return;
        }
        try {
            reviews.createHistoryFromCompletedOrder(order);
        } catch (ReviewConflictException concurrentCreation) {
            if (histories.findByOrderId(order.orderId()).isEmpty()) {
                throw concurrentCreation;
            }
        }
    }
}
//review-end，總共1次修改，第1次//
