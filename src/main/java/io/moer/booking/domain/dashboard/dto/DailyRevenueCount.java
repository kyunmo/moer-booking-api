package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 일별 예약 건수 및 매출 (대시보드 기간별 통계용)
 */
@Getter
@Builder
@AllArgsConstructor
public class DailyRevenueCount {
    private String date;       // yyyy-MM-dd
    private Integer count;     // 예약 건수
    private Long revenue;      // 완료 매출
}
