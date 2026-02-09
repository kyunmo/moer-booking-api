package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 시간대별 예약 건수
 */
@Getter
@Builder
@AllArgsConstructor
public class HourlyCount {
    /**
     * 시간대 (HH:00 형식, 예: "09:00")
     */
    private String hour;

    /**
     * 예약 건수
     */
    private Integer count;
}
