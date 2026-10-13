package com.dinogo.sysmsg.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.sysmsg.repository.RecordRepository;
import com.dinogo.sysmsg.service.mapper.RecordResponseMapper;

@SpringBootTest(properties = "app.sysmsg.reconciliation.enabled=false")
class SystemRecordReadIntegrationTest {
    @Autowired RecordRepository records;
    @Autowired RecordResponseMapper mapper;

    @Test
    @Transactional(readOnly = true)
    void readsAndMapsFirstSystemRecordPageWithoutDuplicateOrderBy() {
        records.findAllByOrderByRecordCreatedAtDescRecordIdDesc(PageRequest.of(0, 20))
                .map(mapper::toResponse);
    }
}
