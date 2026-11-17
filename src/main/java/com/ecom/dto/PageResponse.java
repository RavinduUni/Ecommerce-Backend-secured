package com.ecom.dto;

import java.util.List;

/**
 * One page of results.
 * JSON: { content: [...], page: 0, size: 24, totalElements: 120, totalPages: 5, last: false }
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        boolean last = (long) (page + 1) * size >= totalElements;
        return new PageResponse<>(List.copyOf(content), page, size, totalElements, totalPages, last);
    }
}