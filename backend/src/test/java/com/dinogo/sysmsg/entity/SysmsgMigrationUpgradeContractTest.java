package com.dinogo.sysmsg.entity;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 功能：驗證正式 V009 Sysmsg migration 保留既有資料庫升級所需的條件式建表、欄位與約束補建。
 * 用途：避免單體部署升級既有環境時，因 migration 改動而覆蓋資料或重複建立資料庫物件。
 */
class SysmsgMigrationUpgradeContractTest {

    private static final String MIGRATION_NAME = "V009__create_sysmsg_messaging_schema.sql";

    @Test
    void v006GuardsEveryTableAndAllNamedIndexesAndConstraints() throws Exception {
        Path migration = locateMigration();
        assertTrue(Files.isRegularFile(migration), "找不到 DinoGo 實際 V009 migration: " + migration.toAbsolutePath());
        String sql = Files.readString(migration, StandardCharsets.UTF_8);

        for (String table : new String[] {
                "send", "msg_function_sequence", "send_order", "send_disorder",
                "send_seller", "record", "record_channel"}) {
            assertTrue(sql.contains("OBJECT_ID(N'sysmsg." + table + "', N'U') IS NULL")
                            || sql.contains("OBJECT_ID('sysmsg." + table + "', 'U') IS NULL"),
                    "V009 必須條件式建立 sysmsg." + table);
        }

        for (String index : new String[] {
                "UX_sysmsg_send_msg_function_save", "UX_sysmsg_send_id_msg_function",
                "IX_sysmsg_send_owner", "IX_sysmsg_record_member_category",
                "IX_sysmsg_record_seller_category", "IX_sysmsg_record_send_id",
                "UX_sysmsg_record_id_send_id", "UX_sysmsg_record_order_member_once",
                "UX_sysmsg_record_order_seller_once", "IX_sysmsg_record_msgfrom",
                "IX_sysmsg_record_channel_pending", "IX_sysmsg_record_channel_send"}) {
            assertTrue(sql.contains("name=N'" + index + "'"),
                    "V009 必須先檢查索引 " + index);
        }

        for (String constraint : new String[] {
                "PK_sysmsg_send", "CK_send_msg_function",
                "PK_sysmsg_msg_function_sequence",
                "PK_sysmsg_send_order", "FK_send_order_send",
                "PK_sysmsg_send_disorder", "FK_send_disorder_send",
                "PK_sysmsg_send_seller", "FK_send_seller_send",
                "PK_sysmsg_record", "FK_sysmsg_record_send_function",
                "CK_sysmsg_record_exactly_one_recipient",
                "PK_sysmsg_record_channel", "UX_sysmsg_record_channel_type",
                "FK_sysmsg_record_channel_record_send",
                "CK_sysmsg_record_channel_result"}) {
            assertTrue(sql.contains("name=N'" + constraint + "'"),
                    "V009 必須先檢查約束 " + constraint);
        }

        assertTrue(sql.contains("COL_LENGTH(N'sysmsg.record', N'member_inbox') IS NULL"));
        assertTrue(sql.contains("COL_LENGTH(N'sysmsg.record_channel', N'notification_type') IS NULL"));
        assertTrue(sql.contains("THROW 51010"));
        assertTrue(sql.contains("THROW 51016"));
        assertTrue(sql.contains("status IN ('PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'CANCELLED')"));
        assertTrue(sql.contains("LEFT(msg_function, 2) IN ('AC', 'AS')"));
    }

    private Path locateMigration() {
        Path repositoryRootRun = Path.of("database", "migrations", MIGRATION_NAME);
        if (Files.isRegularFile(repositoryRootRun)) {
            return repositoryRootRun;
        }
        return Path.of("..", "database", "migrations", MIGRATION_NAME);
    }
}
