package com.dinogo.sysmsg.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.sysmsg.entity.SendDisorderEntity;

public interface SendDisorderRepository
        extends JpaRepository<SendDisorderEntity, Integer> {
	
	List<SendDisorderEntity> findByOrderIdOrderBySendIdDesc(Integer orderId);

}
