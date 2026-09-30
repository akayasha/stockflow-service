package com.stockflow.common.page;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Stable JSON shape for paginated list responses. Wraps Spring's {@link Page}
 * so we control the field names - Spring's default serialisation uses
 * {@code pageable} / {@code sort} which we do not want clients to rely on.
 */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }

    public <R> PageResponse<R> map(Function<T, R> mapper) {
        return new PageResponse<>(
            content.stream().map(mapper).toList(),
            page, size, totalElements, totalPages, first, last
        );
    }
}
