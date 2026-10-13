package com.dinogo.sysmsg.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/** 驗證正式 V009 接受完整訂單狀態，並移除非 Sales 正式狀態 COD_PLACED。 */
class SysmsgOrderStatusesMigrationContractTest {

    private static final String MIGRATION_NAME = "V009__create_sysmsg_messaging_schema.sql";
    private static final String ALLOWED_STATUSES =
            "'PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'CANCELLED'";

    @Test
    void migrationAllowsAllSalesStatusesAndLegacyDeliveredProjection() throws Exception {
        Path migration = locateMigration();
        assertTrue(Files.isRegularFile(migration), "找不到 DinoGo 實際 V009 migration: " + migration.toAbsolutePath());
        String sql = Files.readString(migration, StandardCharsets.UTF_8);

        assertTrue(sql.contains(ALLOWED_STATUSES));
        assertFalse(sql.contains("COD_PLACED"));
        assertTrue(sql.contains("ALTER TABLE sysmsg.send_order DROP CONSTRAINT CK_send_order_status"));
        assertTrue(sql.contains("ALTER TABLE sysmsg.record DROP CONSTRAINT CK_sysmsg_record_order_contract"));
        assertTrue(sql.contains("ALTER TABLE sysmsg.record DROP CONSTRAINT CK_sysmsg_record_order_snapshot"));
        assertTrue(sql.contains("BEGIN TRANSACTION"));
        assertTrue(sql.contains("ROLLBACK TRANSACTION"));
    }

    private Path locateMigration() {
        Path repositoryRootRun = Path.of("database", "migrations", MIGRATION_NAME);
        return Files.isRegularFile(repositoryRootRun)
                ? repositoryRootRun
                : Path.of("..", "database", "migrations", MIGRATION_NAME);
    }
}
