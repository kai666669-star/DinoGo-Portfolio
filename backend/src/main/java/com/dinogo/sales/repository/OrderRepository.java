package com.dinogo.sales.repository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderStatus;
import java.time.LocalDateTime;

/** 訂單聚合的持久化介面。 */
public interface OrderRepository extends JpaRepository<Order, Integer> {
    // 因為 orderItems 是 Lazy Loading，列表或詳情映射 DTO 時可能產生 N+1。用 @EntityGraph
    @EntityGraph(attributePaths = { "orderItems", "shipment" })
    List<Order> findByBuyerIdOrderByCreatedAtDesc(Integer buyerId);

    @EntityGraph(attributePaths = { "orderItems", "shipment" })
    Optional<Order> findByOrderIdAndBuyerId(
            Integer orderId,
            Integer buyerId);

    @EntityGraph(attributePaths = { "orderItems", "shipment" })
    Optional<Order> findByOrderIdAndSellerId(
            Integer orderId,
            Integer sellerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT orders
            FROM Order orders
            WHERE orders.orderId = :orderId
              AND orders.sellerId = :sellerId
            """)
    Optional<Order> findForShipmentCreation(
            @Param("orderId") Integer orderId,
            @Param("sellerId") Integer sellerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = { "orderItems", "shipment" })
    @Query("""
            SELECT orders
            FROM Order orders
            WHERE orders.orderId = :orderId
              AND orders.buyerId = :buyerId
            """)
    Optional<Order> findForCancellation(
            @Param("orderId") Integer orderId,
            @Param("buyerId") Integer buyerId);

    @EntityGraph(attributePaths = "orderItems")
    List<Order> findBySellerIdOrderByCreatedAtDesc(Integer sellerId);

    @EntityGraph(attributePaths = "orderItems")
    List<Order> findBySellerIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            Integer sellerId,
            LocalDateTime startAt,
            LocalDateTime endAt);

    boolean existsByBuyerIdAndStatusIn(Integer buyerId, Collection<com.dinogo.sales.entity.OrderStatus> statuses);

    long countByBuyerIdAndMemberCouponId(Integer buyerId, Integer memberCouponId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "payments")
    @Query("""
            SELECT orders
            FROM Order orders
            WHERE orders.orderId = :orderId
              AND orders.buyerId = :buyerId
            """)
    Optional<Order> findForPaymentCreation(
            @Param("orderId") Integer orderId,
            @Param("buyerId") Integer buyerId);

    @Query("""
            SELECT orders.orderId
            FROM Order orders
            WHERE orders.status = :status
              AND orders.createdAt < :deadline
            """)
    List<Integer> findOrderIdsForPaymentExpiry(
            @Param("status") OrderStatus status,
            @Param("deadline") LocalDateTime deadline);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    // Fetch only orderItems here. Fetching orderItems and payments together would
    // join-fetch two List collections and can raise Hibernate's
    // MultipleBagFetchException. PaymentExpiryService accesses payments inside
    // the same transaction, so it can be loaded lazily and safely.
    @EntityGraph(attributePaths = "orderItems")
    @Query("""
            SELECT orders
            FROM Order orders
            WHERE orders.orderId = :orderId
            """)
    Optional<Order> findForPaymentExpiry(@Param("orderId") Integer orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "payments")
    @Query("""
            SELECT orders
            FROM Order orders
            WHERE orders.orderId = :orderId
            """)
    Optional<Order> findForEcpayCallback(@Param("orderId") Integer orderId);

    /**
     * 通知快照一次載入付款、付款方式與物流；orderItems 維持同交易內的單獨 Lazy 查詢，
     * 避免同時 join-fetch 兩個 List 集合造成 MultipleBagFetchException。
     */
    @EntityGraph(attributePaths = { "payments", "payments.paymentMethod", "shipment" })
    @Query("SELECT orders FROM Order orders WHERE orders.orderId = :orderId")
    Optional<Order> findForSysmsgSnapshot(@Param("orderId") Integer orderId);
}
