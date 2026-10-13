package com.dinogo.review.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.dinogo.review.entity.HistoryEntity;
import com.dinogo.review.repository.HistoryRepository;
import com.dinogo.salesii.dto.OrderSysmsgResponse;

//review-start，總共1次修改，第1次//
class ReviewHistoryReconciliationServiceTest {
    private final HistoryRepository histories = mock(HistoryRepository.class);
    private final ReviewService reviews = mock(ReviewService.class);
    private final ReviewHistoryReconciliationService service =
            new ReviewHistoryReconciliationService(histories, reviews);

    @Test
    void completedOrderCreatesOnlyMissingHistory() {
        OrderSysmsgResponse order = order("COMPLETED");
        service.reconcile(order);
        verify(reviews).createHistoryFromCompletedOrder(order);
    }

    @Test
    void existingCompletedHistoryIsNeverRebuiltOrOverwritten() {
        OrderSysmsgResponse order = order("COMPLETED");
        when(histories.findByOrderId(10)).thenReturn(Optional.of(new HistoryEntity()));
        service.reconcile(order);
        verify(reviews, never()).createHistoryFromCompletedOrder(order);
    }

    @Test
    void cancelledOrderRemovesHistoryThroughReviewService() {
        OrderSysmsgResponse order = order("CANCELLED");
        service.reconcile(order);
        verify(reviews).deleteHistoryForCancelledOrder(order);
    }

    @Test
    void notificationProjectionDoesNotReplaceRawReviewStatus() {
        OrderSysmsgResponse order = new OrderSysmsgResponse(
                10, "ORD-10", 7, 9, "DELIVERED", List.of(),
                null, null, null, null, null, null, null,
                "COMPLETED", null, null, "DELIVERED", null, null, null);

        service.reconcile(order);

        verify(reviews).createHistoryFromCompletedOrder(order);
    }

    private OrderSysmsgResponse order(String status) {
        return new OrderSysmsgResponse(10, "ORD-10", 7, 9, status, List.of());
    }
}
//review-end，總共1次修改，第1次//
