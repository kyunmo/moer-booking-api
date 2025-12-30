package io.moer.booking.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {
    private List<T> content;       // 데이터 목록
    private int page;              // 현재 페이지 (1부터 시작)
    private int size;              // 페이지당 개수
    private long totalElements;    // 전체 데이터 개수
    private int totalPages;        // 전체 페이지 수
    private boolean first;         // 첫 페이지 여부
    private boolean last;          // 마지막 페이지 여부

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return PageResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 1)
                .last(page >= totalPages)
                .build();
    }
}