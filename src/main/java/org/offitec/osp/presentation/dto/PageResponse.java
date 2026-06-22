package org.offitec.osp.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Lightweight page envelope returned by the paginated list endpoints. Keeps the
 * response small (only what the frontend needs to render a page and decide whether
 * to offer a "Load more") instead of serializing Spring's full Page object.
 */
@Getter
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
}
