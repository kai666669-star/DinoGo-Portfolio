package com.dinogo.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.SliceImpl;

import com.dinogo.review.service.ReviewHistoryReconciliationService;
import com.dinogo.salesii.service.RecentOrderUpdateQueryService;
import com.dinogo.salesii.dto.OrderSysmsgResponse;
import com.dinogo.salesii.service.OrderSysmsgProviderService;
import com.dinogo.sysmsg.service.OrderMessageService;

//rev+msg-start，總共1次修改，第1次//
class OrderEventReconciliationSchedulerTest {
    @Test
    void scansLastDayAndSysmsgStillRunsWhenReviewFails() {
        RecentOrderUpdateQueryService orders = mock(RecentOrderUpdateQueryService.class);
        OrderSysmsgProviderService snapshots = mock(OrderSysmsgProviderService.class);
        ReviewHistoryReconciliationService reviews = mock(ReviewHistoryReconciliationService.class);
        OrderMessageService messages = mock(OrderMessageService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-01T04:00:00Z"), ZoneOffset.UTC);
        OrderSysmsgResponse snapshot = new OrderSysmsgResponse(
                10, "ORD-10", 7, 9, "COMPLETED", List.of());
        when(orders.findOrderIds(any(), any()))
                .thenReturn(new SliceImpl<>(List.of(10)));
        when(snapshots.getOrderForSysmsg(10)).thenReturn(snapshot);
        doThrow(new IllegalStateException("review down")).when(reviews).reconcile(snapshot);

        new OrderEventReconciliationScheduler(
                orders, snapshots, reviews, messages, clock, 100)
                .reconcileRecentlyUpdatedOrders();

        verify(orders).findOrderIds(
                org.mockito.ArgumentMatchers.eq(LocalDateTime.of(2026, 8, 31, 4, 0)), any());
        verify(messages).createOrderEventMessagesFromSnapshot(snapshot);
    }

    @Test
    void successfulRunAdvancesToOverlappingIncrementalWindow() {
        RecentOrderUpdateQueryService orders = mock(RecentOrderUpdateQueryService.class);
        OrderSysmsgProviderService snapshots = mock(OrderSysmsgProviderService.class);
        ReviewHistoryReconciliationService reviews = mock(ReviewHistoryReconciliationService.class);
        OrderMessageService messages = mock(OrderMessageService.class);
        Clock clock = mock(Clock.class);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(clock.instant()).thenReturn(
                Instant.parse("2026-09-01T04:00:00Z"),
                Instant.parse("2026-09-01T04:01:00Z"));
        when(orders.findOrderIds(any(), any())).thenReturn(new SliceImpl<>(List.of()));
        OrderEventReconciliationScheduler scheduler = new OrderEventReconciliationScheduler(
                orders, snapshots, reviews, messages, clock, 100);

        scheduler.reconcileRecentlyUpdatedOrders();
        scheduler.reconcileRecentlyUpdatedOrders();

        verify(orders).findOrderIds(
                org.mockito.ArgumentMatchers.eq(LocalDateTime.of(2026, 8, 31, 4, 0)), any());
        verify(orders).findOrderIds(
                org.mockito.ArgumentMatchers.eq(LocalDateTime.of(2026, 9, 1, 3, 55)), any());
        verify(orders, times(2)).findOrderIds(any(), any());
    }
}
//rev+msg-end，總共1次修改，第1次//
