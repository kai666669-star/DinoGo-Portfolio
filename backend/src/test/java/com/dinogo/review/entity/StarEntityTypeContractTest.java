package com.dinogo.review.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

//review-start，總共1次修改，第1次//
/** 鎖定 Integer fiveStar 必須以 JDBC TINYINT 驗證 SQL Server schema。 */
class StarEntityTypeContractTest {

    @Test
    void fiveStarUsesTinyIntJdbcType() throws Exception {
        JdbcTypeCode mapping = StarEntity.class
                .getDeclaredField("fiveStar")
                .getAnnotation(JdbcTypeCode.class);
        assertEquals(SqlTypes.TINYINT, mapping.value());
    }
}
//review-end，總共1次修改，第1次//
