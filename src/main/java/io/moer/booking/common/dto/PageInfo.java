package io.moer.booking.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PageInfo {
    private int page;           // 현재 페이지 (1부터 시작)
    private int size;           // 페이지당 개수
    private long totalElements; // 전체 데이터 개수
    private int totalPages;     // 전체 페이지 수

    public PageInfo(int page, int size, long totalElements) {
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / size);
    }
}