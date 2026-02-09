package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 월별 매출 데이터 (그래프용)
 */
@Getter
@Builder
@AllArgsConstructor
public class MonthlyRevenue {
    /**
     * 년월 (yyyy-MM)
     */
    private String yearMonth;

    /**
     * 매출
     */
    private Integer revenue;

    /**
     * 예약 건수
     */
    private Integer reservationCount;
}
