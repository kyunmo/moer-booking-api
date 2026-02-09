package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 직원별 성과
 */
@Getter
@Builder
@AllArgsConstructor
public class StaffPerformance {
    private Long staffId;
    private String staffName;
    private Integer reservationCount;
    private Integer totalRevenue;
    private Integer averageDuration;
}
