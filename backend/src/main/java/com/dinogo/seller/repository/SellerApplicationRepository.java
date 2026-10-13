package com.dinogo.seller.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.seller.entity.SellerApplication;
import com.dinogo.seller.entity.SellerApplicationStatus;

public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Integer> {

    Optional<SellerApplication> findTopByMember_MemberIdOrderByCreatedAtDesc(Integer memberId);

    List<SellerApplication> findAllByOrderByCreatedAtDesc();

    List<SellerApplication> findByStatusOrderByCreatedAtDesc(SellerApplicationStatus status);

    boolean existsByMember_MemberIdAndStatusIn(
            Integer memberId,
            Collection<SellerApplicationStatus> statuses);
}
