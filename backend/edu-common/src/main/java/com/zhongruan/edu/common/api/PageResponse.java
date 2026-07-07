package com.zhongruan.edu.common.api;

import java.util.List;

public record PageResponse<T>(List<T> records, long page, long size, long total, long totalPages) {
    public PageResponse {
        records = records == null ? List.of() : List.copyOf(records);
        if (page < 1) {
            throw new IllegalArgumentException("page must be at least 1");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
        if (total < 0 || totalPages < 0) {
            throw new IllegalArgumentException("totals cannot be negative");
        }
    }

    public static <T> PageResponse<T> of(List<T> records, long page, long size, long total) {
        long totalPages = total == 0 ? 0 : (total + size - 1) / size;
        return new PageResponse<>(records, page, size, total, totalPages);
    }
}

