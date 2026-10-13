package com.dinogo.sysmsg.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.dinogo.sysmsg.entity.MsgFunctionSequenceEntity;

import jakarta.persistence.LockModeType;

public interface MsgFunctionSequenceRepository
        extends JpaRepository<MsgFunctionSequenceEntity, String> {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MsgFunctionSequenceEntity> findById(String prefix);
}
