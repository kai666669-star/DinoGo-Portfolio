package com.dinogo.seller.repository;

import java.math.BigDecimal;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dinogo.seller.entity.WithdrawalRequest;
import com.dinogo.seller.entity.WithdrawalStatus;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Integer> {

    @Query("""
            select sum(w.amount)
            from WithdrawalRequest w
            where w.seller.sellerId = :sellerId and w.status in :statuses
            """)
    BigDecimal sumAmountBySellerIdAndStatusIn(
            @Param("sellerId") Integer sellerId,
            @Param("statuses") Collection<WithdrawalStatus> statuses);
}
