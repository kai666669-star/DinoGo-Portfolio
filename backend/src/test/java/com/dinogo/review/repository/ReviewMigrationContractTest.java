package com.dinogo.review.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

//review-start，總共1次修改，第1次//
/** 鎖定 Review Entity 與 V008 的 tinyint、0～5 priority 及 Offset 索引契約。 */
class ReviewMigrationContractTest {

    @Test
    void migrationMatchesFinalReviewModel() throws Exception {
        Path migration = Path.of("..", "database", "migrations", "V008__create_review_history_and_star.sql");
        String sql = Files.readString(migration).replaceAll("\\s+", " ").toLowerCase();
        assertTrue(sql.contains("five_star tinyint"));
        assertTrue(sql.contains("then convert(tinyint, 5)"));
        assertTrue(sql.contains("review_priority desc"));
        assertTrue(sql.contains("where five_star is not null"));
    }
}
//review-end，總共1次修改，第1次//
