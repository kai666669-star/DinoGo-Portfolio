package com.dinogo.integration;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dinogo.review.service.ReviewHistoryReconciliationService;
import com.dinogo.salesii.service.RecentOrderUpdateQueryService;
import com.dinogo.salesii.service.OrderSysmsgProviderService;
import com.dinogo.sysmsg.service.OrderMessageService;

//rev+msg-start，總共1次修改，第1次//
/**
 * 功能：每分鐘分頁自檢最近 24 小時更新的訂單聚合。
 * 應用：補正 AFTER_COMMIT 非同步事件因程式關閉或短暫失敗而遺漏的 Review/sysmsg 資料。
 */
@Component
@ConditionalOnProperty(
        name = "app.sysmsg.reconciliation.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OrderEventReconciliationScheduler {
    private static final Logger LOG = Logger.getLogger(OrderEventReconciliationScheduler.class.getName());

    private final RecentOrderUpdateQueryService recentOrders;
    private final OrderSysmsgProviderService snapshots;
    private final ReviewHistoryReconciliationService reviews;
    private final OrderMessageService messages;
    private final Clock clock;
    private final int pageSize;
    private final Duration overlap;
    private final int fullSweepEveryRuns;
    private LocalDateTime lastSuccessfulRunStartedAt;
    private int runsSinceFullSweep;

    @Autowired
    public OrderEventReconciliationScheduler(
            RecentOrderUpdateQueryService recentOrders,
            OrderSysmsgProviderService snapshots,
            ReviewHistoryReconciliationService reviews,
            OrderMessageService messages,
            @Value("${sysmsg.reconciliation.page-size:100}") int pageSize,
            @Value("${sysmsg.reconciliation.overlap-minutes:5}") long overlapMinutes,
            @Value("${sysmsg.reconciliation.full-sweep-every-runs:60}") int fullSweepEveryRuns) {
        this(recentOrders, snapshots, reviews, messages, Clock.systemDefaultZone(), pageSize,
                Duration.ofMinutes(Math.max(0, overlapMinutes)), fullSweepEveryRuns);
    }

    OrderEventReconciliationScheduler(
            RecentOrderUpdateQueryService recentOrders,
            OrderSysmsgProviderService snapshots,
            ReviewHistoryReconciliationService reviews,
            OrderMessageService messages,
            Clock clock,
            int pageSize) {
        this(recentOrders, snapshots, reviews, messages, clock, pageSize, Duration.ofMinutes(5), 60);
    }

    OrderEventReconciliationScheduler(
            RecentOrderUpdateQueryService recentOrders,
            OrderSysmsgProviderService snapshots,
            ReviewHistoryReconciliationService reviews,
            OrderMessageService messages,
            Clock clock,
            int pageSize,
            Duration overlap,
            int fullSweepEveryRuns) {
        this.recentOrders = recentOrders;
        this.snapshots = snapshots;
        this.reviews = reviews;
        this.messages = messages;
        this.clock = clock;
        this.pageSize = Math.max(1, pageSize);
        this.overlap = overlap.isNegative() ? Duration.ZERO : overlap;
        this.fullSweepEveryRuns = Math.max(1, fullSweepEveryRuns);
    }

    @Scheduled(fixedDelayString = "${sysmsg.reconciliation.fixed-delay-ms:60000}")
    public void reconcileRecentlyUpdatedOrders() {
        LocalDateTime runStartedAt = LocalDateTime.now(clock);
        boolean fullSweep = lastSuccessfulRunStartedAt == null
                || runsSinceFullSweep >= fullSweepEveryRuns - 1;
        LocalDateTime since = fullSweep
                ? runStartedAt.minusDays(1)
                : lastSuccessfulRunStartedAt.minus(overlap);
        int pageNumber = 0;
        Slice<Integer> page;
        boolean allSuccessful = true;
        do {
            page = recentOrders.findOrderIds(
                    since, PageRequest.of(pageNumber, pageSize));
            for (Integer orderId : page.getContent()) {
                allSuccessful &= reconcileOne(orderId);
            }
            pageNumber++;
        } while (page.hasNext());
        if (allSuccessful) {
            lastSuccessfulRunStartedAt = runStartedAt;
            runsSinceFullSweep = fullSweep ? 0 : runsSinceFullSweep + 1;
        }
    }

    private boolean reconcileOne(Integer orderId) {
        com.dinogo.salesii.dto.OrderSysmsgResponse snapshot;
        try {
            snapshot = snapshots.getOrderForSysmsg(orderId);
        } catch (RuntimeException exception) {
            LOG.log(Level.WARNING, "Order snapshot reconciliation failed for order " + orderId, exception);
            return false;
        }
        boolean successful = true;
        try {
            reviews.reconcile(snapshot);
        } catch (RuntimeException exception) {
            LOG.log(Level.WARNING, "Review reconciliation failed for order " + orderId, exception);
            successful = false;
        }
        try {
            messages.createOrderEventMessagesFromSnapshot(snapshot);
        } catch (RuntimeException exception) {
            LOG.log(Level.WARNING, "Sysmsg reconciliation failed for order " + orderId, exception);
            successful = false;
        }
        return successful;
    }
}
//rev+msg-end，總共1次修改，第1次//
