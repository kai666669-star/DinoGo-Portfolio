package com.dinogo.sysmsg.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

/** Offset 分頁統一回應；每頁固定 10 筆，頁碼從 0 開始。 */
public record OffsetPageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {

    public static <T> OffsetPageResponse<T> from(Page<T> source) {
        return new OffsetPageResponse<>(
                source.getContent(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.hasNext());
    }
}
