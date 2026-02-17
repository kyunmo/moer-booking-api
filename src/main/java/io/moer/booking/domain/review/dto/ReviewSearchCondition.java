package io.moer.booking.domain.review.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 리뷰 검색 조건 (Admin)
 */
@Getter
@Builder
public class ReviewSearchCondition {
    private Long businessId;
    private String status;
    private Integer rating;
    private Long staffId;
    private LocalDate startDate;
    private LocalDate endDate;
    private int page;
    private int size;

    /**
     * 페이징 offset 계산
     */
    public int getOffset() {
        return (page - 1) * size;
    }
}
