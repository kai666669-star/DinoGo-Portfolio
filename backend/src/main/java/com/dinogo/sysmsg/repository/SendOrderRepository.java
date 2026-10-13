package com.dinogo.sysmsg.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.sysmsg.entity.SendOrderEntity;

public interface SendOrderRepository
        extends JpaRepository<SendOrderEntity, Integer> {

    // 依 order_id 找 PAID / SHIPPED / DELIVERED / COMPLETED
    List<SendOrderEntity> findByOrderIdOrderBySendIdDesc(Integer orderId);

    List<SendOrderEntity> findByOrderIdAndStatusOrderBySendIdDesc(
            Integer orderId,
            String status
    );
}
