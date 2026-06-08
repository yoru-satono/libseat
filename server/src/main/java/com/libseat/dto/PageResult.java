package com.libseat.dto;

import java.util.List;

public record PageResult<T>(
        List<T> items,
        long total,
        int page,
        int pageSize,
        int totalPages
) {
    public static <T> PageResult<T> of(List<T> items, long total, int page, int pageSize) {
        int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        return new PageResult<>(items, total, page, pageSize, totalPages);
    }
}
