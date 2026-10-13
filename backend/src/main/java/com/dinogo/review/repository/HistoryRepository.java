package com.dinogo.review.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.review.entity.HistoryEntity;

/** History 資料存取層只宣告查詢方法，不放會員權限或業務流程。 */

public interface HistoryRepository extends JpaRepository<HistoryEntity, Integer> {

    /** 依訂單 ID 查詢；也用於防止同一張訂單重複建立評論主檔。 */
    Optional<HistoryEntity> findByOrderId(Integer orderId);
}
