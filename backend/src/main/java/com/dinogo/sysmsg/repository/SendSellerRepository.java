package com.dinogo.sysmsg.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.sysmsg.entity.SendSellerEntity;

public interface SendSellerRepository
        extends JpaRepository<SendSellerEntity, Integer> {
}
