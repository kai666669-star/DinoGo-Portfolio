package com.dinogo.sales.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.dinogo.sales.entity.ShipmentEvent;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, Integer> {
    List<ShipmentEvent> findByShipmentShipmentIdOrderByOccurredAtAsc(Integer shipmentId);
}
