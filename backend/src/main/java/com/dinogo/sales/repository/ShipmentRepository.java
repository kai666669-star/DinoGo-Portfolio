package com.dinogo.sales.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dinogo.sales.entity.Shipment;

import jakarta.persistence.LockModeType;

public interface ShipmentRepository extends JpaRepository<Shipment, Integer> {

    boolean existsByOrderOrderId(Integer orderId);

    @EntityGraph(attributePaths = "order")
    Optional<Shipment> findByOrderOrderId(Integer orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "order")
    @Query("""
            SELECT shipment
            FROM Shipment shipment
            WHERE shipment.order.orderId = :orderId
              AND shipment.order.sellerId = :sellerId
            """)
    Optional<Shipment> findForStatusUpdate(
            @Param("orderId") Integer orderId,
            @Param("sellerId") Integer sellerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "order")
    @Query("""
            SELECT shipment
            FROM Shipment shipment
            WHERE shipment.order.orderId = :orderId
              AND shipment.order.buyerId = :buyerId
            """)
    Optional<Shipment> findForDeliveryConfirmation(
            @Param("orderId") Integer orderId,
            @Param("buyerId") Integer buyerId);
}
