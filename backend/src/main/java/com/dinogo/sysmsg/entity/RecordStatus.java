package com.dinogo.sysmsg.entity;



/**
 * ============================================================
 * sysmsg.record.record_status
 * ============================================================
 *
 * UNREAD = 未讀
 * READ   = 已讀
 * DELETE = 收件匣刪除
 *
 * SQL Server 同時有：
 *
 * CHECK ([record_status] IN
 * ('UNREAD', 'READ', 'DELETE'))
 *
 * 因此：
 * Java Enum      → Spring Boot 型別控制
 * SQL CHECK      → SQL Server DB 保護
 *
 * 如果使用 Lombok：
 *
 * @Getter
 *
 * 可以自動產生 Getter。
 *
 * 本專案不實際使用 Lombok。
 */
public enum RecordStatus {

    UNREAD,
    READ,
    DELETE
}
