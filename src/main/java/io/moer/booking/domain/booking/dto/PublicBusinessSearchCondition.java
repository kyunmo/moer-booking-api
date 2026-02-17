package io.moer.booking.domain.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객용 매장 검색 조건
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicBusinessSearchCondition {

    /**
     * 검색 키워드 (매장명, 주소)
     */
    private String keyword;

    /**
     * 업종 필터 (BEAUTY_SHOP, PILATES 등)
     */
    private String businessType;

    /**
     * 정렬 기준 (rating, name, created_at)
     * 기본값: rating
     */
    private String sortBy;

    /**
     * 페이지 번호 (1부터 시작)
     * 기본값: 1
     */
    private Integer page;

    /**
     * 페이지 크기
     * 기본값: 20
     */
    private Integer size;

    public int getPageOrDefault() {
        return (page == null || page < 1) ? 1 : page;
    }

    public int getSizeOrDefault() {
        return (size == null || size < 1) ? 20 : Math.min(size, 100);
    }

    public String getSortByOrDefault() {
        return (sortBy == null || sortBy.isBlank()) ? "rating" : sortBy;
    }

    public int getOffset() {
        return (getPageOrDefault() - 1) * getSizeOrDefault();
    }
}
