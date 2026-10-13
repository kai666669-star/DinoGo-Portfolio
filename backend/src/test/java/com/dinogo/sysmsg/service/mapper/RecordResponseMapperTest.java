package com.dinogo.sysmsg.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.dinogo.sysmsg.entity.RecordEntity;
import com.dinogo.sysmsg.entity.SendDisorderEntity;
import com.dinogo.sysmsg.entity.SendOrderEntity;
import com.dinogo.sysmsg.entity.SendStatus;

class RecordResponseMapperTest {
    private final RecordResponseMapper mapper = new RecordResponseMapper();

    @Test
    void exposesSendOrderSnapshotOrderNoInInboxAndDetail() {
        SendOrderEntity send = new SendOrderEntity(
                1, "AC-001", "ORD-20260904-001", "訂單付款成功", "content", SendStatus.SEND,
                65, "ORD-20260904-001", new BigDecimal("2500"), 1, "信用卡",
                LocalDateTime.of(2026, 9, 4, 21, 15), "PAID");
        RecordEntity record = new RecordEntity(send, 18, null);

        assertEquals("ORD-20260904-001", mapper.toInboxResponse(record).getOrderNo());
        assertEquals(new BigDecimal("2500"), mapper.toInboxResponse(record).getTotalAmount());
        assertEquals("信用卡", mapper.toInboxResponse(record).getPaymentMethodName());
        assertEquals("ORD-20260904-001", mapper.toResponse(record).getOrderNo());
    }

    @Test
    void exposesSendDisorderSnapshotOrderNoInInboxAndDetail() {
        SendDisorderEntity send = new SendDisorderEntity(
                1, "AC-002", "ORD-20260904-002", "訂單已取消", "content", SendStatus.SEND,
                66, "ORD-20260904-002", new BigDecimal("680"), 2, "貨到付款",
                "會員取消", LocalDateTime.of(2026, 9, 4, 22, 10), "CANCELLED");
        RecordEntity record = new RecordEntity(send, 19, null);

        assertEquals("ORD-20260904-002", mapper.toInboxResponse(record).getOrderNo());
        assertEquals(new BigDecimal("680"), mapper.toInboxResponse(record).getTotalAmount());
        assertEquals("貨到付款", mapper.toInboxResponse(record).getPaymentMethodName());
        assertEquals("ORD-20260904-002", mapper.toResponse(record).getOrderNo());
    }
}
