package com.dinogo.integration;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.dinogo.review.service.ReviewHistoryReconciliationService;
import com.dinogo.salesii.event.OrderStatusChangedEvent;
import com.dinogo.salesii.service.OrderSysmsgProviderService;

//review-start，總共1次修改，第1次//
/**
 * 單體部署的 Sales → Review 接線。
 * Sales 交易提交後才非同步執行；Review 失敗不影響已提交訂單。
 * 每分鐘自檢會重新取得權威快照，補齊運行中遺漏的 History。
 */
@Component
public class ReviewOrderStatusChangedListener {
    private final OrderSysmsgProviderService orders;
    private final ReviewHistoryReconciliationService reviews;

    public ReviewOrderStatusChangedListener(
            OrderSysmsgProviderService orders,
            ReviewHistoryReconciliationService reviews) {
        this.orders = orders;
        this.reviews = reviews;
    }

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = false)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        reviews.reconcile(orders.getOrder(event.orderId()));
    }
}
//review-end，總共1次修改，第1次//
