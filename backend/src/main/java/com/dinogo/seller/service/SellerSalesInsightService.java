package com.dinogo.seller.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderItem;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.seller.dto.SellerSalesInsightResponse;
import com.dinogo.seller.dto.SellerSalesInsightStats;
import com.dinogo.seller.dto.SellerSalesInsightStats.ProductSalesSummary;
import com.dinogo.seller.service.SalesInsightLlmClient.LlmResult;

@Service
public class SellerSalesInsightService {

    private final CurrentSellerService currentSellerService;
    private final OrderRepository orderRepository;
    private final SalesInsightLlmClient llmClient;

    public SellerSalesInsightService(
            CurrentSellerService currentSellerService,
            OrderRepository orderRepository,
            SalesInsightLlmClient llmClient) {
        this.currentSellerService = currentSellerService;
        this.orderRepository = orderRepository;
        this.llmClient = llmClient;
    }

    @Transactional(readOnly = true)
    public SellerSalesInsightResponse getStats(
            Integer memberId,
            LocalDate startDate,
            LocalDate endDate) {
        SellerSalesInsightStats stats = buildStats(memberId, startDate, endDate);
        return new SellerSalesInsightResponse(
                stats,
                null,
                false,
                null,
                LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public SellerSalesInsightResponse analyze(
            Integer memberId,
            LocalDate startDate,
            LocalDate endDate) {
        SellerSalesInsightStats stats = buildStats(memberId, startDate, endDate);
        LlmResult llmResult = llmClient.generateInsight(stats);
        return new SellerSalesInsightResponse(
                stats,
                llmResult.content(),
                llmResult.generatedByAi(),
                llmResult.modelName(),
                LocalDateTime.now());
    }

    private SellerSalesInsightStats buildStats(
            Integer memberId,
            LocalDate startDate,
            LocalDate endDate) {
        DateRange currentRange = resolveCurrentRange(startDate, endDate);
        DateRange previousRange = currentRange.previousRange();
        Integer sellerId = currentSellerService.requireActiveSellerId(memberId);
        List<Order> orders = orderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);

        PeriodMetrics current = calculateMetrics(orders, currentRange);
        PeriodMetrics previous = calculateMetrics(orders, previousRange);
        return new SellerSalesInsightStats(
                currentRange.start(),
                currentRange.end(),
                previousRange.start(),
                previousRange.end(),
                current.revenueAmount(),
                current.orderCount(),
                current.averageOrderValue(),
                current.soldQuantity(),
                previous.revenueAmount(),
                previous.orderCount(),
                previous.averageOrderValue(),
                previous.soldQuantity(),
                calculateChangeRate(current.revenueAmount(), previous.revenueAmount()),
                calculateChangeRate(current.orderCount(), previous.orderCount()),
                calculateChangeRate(current.averageOrderValue(), previous.averageOrderValue()),
                current.topProducts(),
                current.lowProducts());
    }

    private DateRange resolveCurrentRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            LocalDate today = LocalDate.now();
            return new DateRange(today.minusDays(6), today);
        }
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("查詢結束日期不可早於開始日期，且日期不可為空");
        }
        return new DateRange(startDate, endDate);
    }

    private PeriodMetrics calculateMetrics(List<Order> orders, DateRange range) {
        List<Order> countedOrders = orders.stream()
                .filter(order -> isCountedSalesOrder(order.getStatus()))
                .filter(order -> isDateInRange(order.getCreatedAt(), range))
                .toList();

        List<Order> completedOrders = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED)
                .filter(order -> isDateInRange(order.getCompletedAt(), range))
                .toList();

        BigDecimal revenueAmount = completedOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int orderCount = countedOrders.size();
        int soldQuantity = countedOrders.stream()
                .mapToInt(this::sumOrderQuantity)
                .sum();
        BigDecimal averageOrderValue = orderCount == 0
                ? BigDecimal.ZERO
                : revenueAmount.divide(BigDecimal.valueOf(orderCount), 0, RoundingMode.HALF_UP);

        return new PeriodMetrics(
                revenueAmount,
                orderCount,
                averageOrderValue,
                soldQuantity,
                buildTopProducts(countedOrders),
                buildLowProducts(countedOrders));
    }

    private List<ProductSalesSummary> buildTopProducts(List<Order> orders) {
        return buildProductSummaries(orders).stream()
                .sorted(Comparator.comparing(ProductSalesSummary::revenueAmount).reversed())
                .limit(3)
                .toList();
    }

    private List<ProductSalesSummary> buildLowProducts(List<Order> orders) {
        return buildProductSummaries(orders).stream()
                .sorted(Comparator.comparing(ProductSalesSummary::revenueAmount)
                        .thenComparing(ProductSalesSummary::soldQuantity))
                .limit(3)
                .toList();
    }

    private List<ProductSalesSummary> buildProductSummaries(List<Order> orders) {
        return orders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .collect(Collectors.groupingBy(OrderItem::getProductId))
                .entrySet()
                .stream()
                .map(this::toTopProduct)
                .toList();
    }

    private ProductSalesSummary toTopProduct(Map.Entry<Integer, List<OrderItem>> entry) {
        List<OrderItem> items = entry.getValue();
        BigDecimal revenueAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int soldQuantity = items.stream()
                .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                .sum();

        return new ProductSalesSummary(
                entry.getKey(),
                items.get(0).getProductName(),
                revenueAmount,
                soldQuantity,
                items.size());
    }

    private boolean isCountedSalesOrder(OrderStatus status) {
        return status == OrderStatus.PAID
                || status == OrderStatus.PROCESSING
                || status == OrderStatus.SHIPPED
                || status == OrderStatus.COMPLETED;
    }

    private boolean isDateInRange(LocalDateTime dateTime, DateRange range) {
        if (dateTime == null) {
            return false;
        }

        LocalDate date = dateTime.toLocalDate();
        return !date.isBefore(range.start()) && !date.isAfter(range.end());
    }

    private int sumOrderQuantity(Order order) {
        return order.getOrderItems().stream()
                .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                .sum();
    }

    private BigDecimal calculateChangeRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current == null || current.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(100);
        }

        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateChangeRate(Integer current, Integer previous) {
        return calculateChangeRate(
                BigDecimal.valueOf(current == null ? 0 : current),
                BigDecimal.valueOf(previous == null ? 0 : previous));
    }

    private record DateRange(LocalDate start, LocalDate end) {
        DateRange previousRange() {
            long days = ChronoUnit.DAYS.between(start, end) + 1;
            LocalDate previousEnd = start.minusDays(1);
            return new DateRange(previousEnd.minusDays(days - 1), previousEnd);
        }
    }

    private record PeriodMetrics(
            BigDecimal revenueAmount,
            Integer orderCount,
            BigDecimal averageOrderValue,
            Integer soldQuantity,
            List<ProductSalesSummary> topProducts,
            List<ProductSalesSummary> lowProducts) {
    }
}
