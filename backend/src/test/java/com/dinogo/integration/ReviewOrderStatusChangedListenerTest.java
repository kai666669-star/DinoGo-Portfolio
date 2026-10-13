package com.dinogo.integration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dinogo.review.service.ReviewHistoryReconciliationService;
import com.dinogo.salesii.dto.OrderSysmsgResponse;
import com.dinogo.salesii.event.OrderStatusChangedEvent;
import com.dinogo.salesii.service.OrderSysmsgProviderService;

//review-start，總共1次修改，第1次//
class ReviewOrderStatusChangedListenerTest {
    private final OrderSysmsgProviderService orders = mock(OrderSysmsgProviderService.class);
    private final ReviewHistoryReconciliationService reviews = mock(ReviewHistoryReconciliationService.class);
    private final ReviewOrderStatusChangedListener listener =
            new ReviewOrderStatusChangedListener(orders, reviews);

    @Test
    void completedEventReadsOrderOnceAndPassesSameSnapshotToReview() {
        OrderSysmsgResponse order = order("COMPLETED");
        when(orders.getOrder(10)).thenReturn(order);

        listener.onOrderStatusChanged(new OrderStatusChangedEvent(10));

        verify(orders).getOrder(10);
        verify(reviews).reconcile(order);
        verifyNoMoreInteractions(orders, reviews);
    }

    @Test
    void cancelledEventReadsOrderOnceAndPassesSameSnapshotToReview() {
        OrderSysmsgResponse order = order("CANCELLED");
        when(orders.getOrder(10)).thenReturn(order);

        listener.onOrderStatusChanged(new OrderStatusChangedEvent(10));

        verify(orders).getOrder(10);
        verify(reviews).reconcile(order);
        verifyNoMoreInteractions(orders, reviews);
    }

    private OrderSysmsgResponse order(String status) {
        return new OrderSysmsgResponse(10, "ORD-10", 7, 9, status, List.of());
    }
}
//review-end，總共1次修改，第1次//

