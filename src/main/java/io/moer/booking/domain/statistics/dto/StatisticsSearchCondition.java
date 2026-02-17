package io.moer.booking.domain.statistics.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 통계 분석 공통 검색 조건 DTO
 * - 모든 통계 API에서 공통으로 사용
 * - Query parameter 바인딩을 위해 Setter 사용
 */
@Getter
@Setter
public class StatisticsSearchCondition {

    @NotNull(message = "시작일은 필수입니다")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다")
    private LocalDate endDate;

    /** 그룹핑 기준: daily, weekly, monthly */
    private String groupBy;

    /** 비교 대상: PREVIOUS_PERIOD, LAST_YEAR */
    private String compareWith;

    /** 스태프별 필터 (스태프 통계용) */
    private Long staffId;

    /** 카테고리별 필터 (서비스 통계용) */
    private Long categoryId;
}
