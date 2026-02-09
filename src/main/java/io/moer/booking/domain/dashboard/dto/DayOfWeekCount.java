package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 요일별 예약 건수
 */
@Getter
@Builder
@AllArgsConstructor
public class DayOfWeekCount {
    /**
     * 요일 (1=월요일, 7=일요일)
     */
    private Integer dayOfWeek;

    /**
     * 요일명 (월, 화, 수, 목, 금, 토, 일)
     */
    private String dayName;

    /**
     * 예약 건수
     */
    private Integer count;
}
