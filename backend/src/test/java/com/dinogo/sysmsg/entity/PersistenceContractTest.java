package com.dinogo.sysmsg.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

class PersistenceContractTest {
    @Test
    void jpaMappingKeepsSchemasTablesAndSellerOrderSnapshot() throws Exception {
        Table sendTable = SendEntity.class.getAnnotation(Table.class);
        Table recordChannelTable = RecordChannelEntity.class.getAnnotation(Table.class);
        Column orderNo = SendSellerEntity.class.getDeclaredField("orderNo").getAnnotation(Column.class);
        Column attemptCount = RecordChannelEntity.class
                .getDeclaredField("attemptCount")
                .getAnnotation(Column.class);
        Column failureCode = RecordChannelEntity.class
                .getDeclaredField("failureCode")
                .getAnnotation(Column.class);

        assertEquals("sysmsg", sendTable.schema());
        assertEquals("send", sendTable.name());
        assertEquals("record_channel", recordChannelTable.name());
        assertEquals("order_no", orderNo.name());
        assertEquals(30, orderNo.length());
        assertEquals("attempt_count", attemptCount.name());
        assertEquals(false, attemptCount.nullable());
        assertEquals("failure_code", failureCode.name());
        assertEquals(50, failureCode.length());
    }

    @Test
    void entityLifecycleRejectsPrefixSubtypeAndUnsavedTemplateRecord() throws Exception {
        SendEntity invalidScParent = new SendEntity(
                1, "SC-001", "label", "title", "content", SendStatus.SEND);
        InvocationTargetException subtypeFailure = assertThrows(
                InvocationTargetException.class, () -> invoke(invalidScParent, "prePersist"));
        assertTrue(subtypeFailure.getCause().getMessage().contains("send_seller"));

        SendEntity saveTemplate = new SendEntity(
                1, "OA-001", "label", "title", "content", SendStatus.SAVE);
        RecordEntity record = new RecordEntity(saveTemplate, 7, null);
        InvocationTargetException recordFailure = assertThrows(
                InvocationTargetException.class, () -> invoke(record, "prePersist"));
        assertTrue(recordFailure.getCause().getMessage().contains("send_status=SEND"));
    }

    @Test
    void sqlScriptsDeclareRequiredChecksForeignKeysAndUniqueIndexes() throws Exception {
        String sendSql = Files.readString(Path.of("sysmsg基本資料", "SendEntity.sql"));
        String recordSql = Files.readString(Path.of("sysmsg基本資料", "RecordEntity.sql"));
        String channelSql = Files.readString(Path.of("sysmsg基本資料", "RecordChannelEntity.sql"));

        assertTrue(sendSql.contains("CK_send_msg_function"));
        assertTrue(sendSql.contains("UX_sysmsg_send_msg_function_save"));
        assertTrue(recordSql.contains("CK_sysmsg_record_exactly_one_recipient"));
        assertTrue(recordSql.contains("UX_sysmsg_record_order_member_once"));
        assertTrue(recordSql.contains("UX_sysmsg_record_order_seller_once"));
        assertTrue(channelSql.contains("UX_sysmsg_record_channel_type"));
        assertTrue(channelSql.contains("FK_sysmsg_record_channel_record_send"));
        assertTrue(channelSql.contains("attempt_count INT NOT NULL"));
        assertTrue(channelSql.contains("IX_sysmsg_record_channel_retry_due"));
        assertTrue(channelSql.contains("IX_sysmsg_record_channel_dead_letter"));
    }

    private void invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }
}
