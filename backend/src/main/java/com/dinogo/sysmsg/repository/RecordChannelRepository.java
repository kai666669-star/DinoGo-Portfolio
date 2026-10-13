package com.dinogo.sysmsg.repository;

import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.dinogo.sysmsg.entity.ChannelType;
import com.dinogo.sysmsg.entity.RecordChannelEntity;

public interface RecordChannelRepository extends JpaRepository<RecordChannelEntity, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from RecordChannelEntity c where c.recordChannelId = :id")
    Optional<RecordChannelEntity> findByIdForUpdate(@Param("id") Integer id);

    Optional<RecordChannelEntity> findByRecordIdAndChannelType(
            Integer recordId, ChannelType channelType);

    Slice<RecordChannelEntity>
    findByChannelTypeAndSentAtIsNullAndProviderMessageIdIsNullAndErrorMessageIsNullAndAttemptCountAndLastAttemptAtIsNullOrderByRecordChannelIdAsc(
            ChannelType channelType, int attemptCount, Pageable pageable);

    Slice<RecordChannelEntity>
    findByChannelTypeAndSentAtIsNullAndProviderMessageIdIsNullAndErrorMessageIsNotNullAndNextRetryAtLessThanEqualAndDeadLetteredAtIsNullOrderByNextRetryAtAscRecordChannelIdAsc(
            ChannelType channelType, LocalDateTime now, Pageable pageable);
}
