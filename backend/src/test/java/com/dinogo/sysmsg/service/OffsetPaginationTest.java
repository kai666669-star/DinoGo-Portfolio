package com.dinogo.sysmsg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.dinogo.sysmsg.dto.response.OffsetPageResponse;

/** 驗證統一 Offset 回應欄位與固定十筆分頁合約。 */
class OffsetPaginationTest {

    @Test
    void mapsSpringPageMetadataWithoutCursor() {
        PageRequest request = PageRequest.of(1, 10);
        OffsetPageResponse<Integer> response = OffsetPageResponse.from(
                new PageImpl<>(List.of(11, 12), request, 12));

        assertEquals(List.of(11, 12), response.items());
        assertEquals(1, response.page());
        assertEquals(10, response.size());
        assertEquals(12, response.totalElements());
        assertEquals(2, response.totalPages());
        assertFalse(response.hasNext());
    }

    @Test
    void firstPageReportsNextPageWhenMoreThanTenRowsExist() {
        OffsetPageResponse<Integer> response = OffsetPageResponse.from(
                new PageImpl<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                        PageRequest.of(0, 10), 11));

        assertTrue(response.hasNext());
        assertEquals(10, response.size());
    }
}
