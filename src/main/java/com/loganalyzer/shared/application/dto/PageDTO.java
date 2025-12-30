package com.loganalyzer.shared.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 分页响应DTO
 */
@Data
@Builder
public class PageDTO<T> {
    private List<T> content;      // 数据列表
    private int page;             // 当前页码（从0开始）
    private int size;             // 每页大小
    private long totalElements;   // 总元素数
    private int totalPages;       // 总页数
    private boolean first;        // 是否第一页
    private boolean last;         // 是否最后一页
    
    public static <T> PageDTO<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return PageDTO.<T>builder()
            .content(content)
            .page(page)
            .size(size)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .first(page == 0)
            .last(page >= totalPages - 1)
            .build();
    }
}
