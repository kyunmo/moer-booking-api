package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 시간대별 분석
 */
@Getter
@Builder
@AllArgsConstructor
public class TimeSlotAnalysis {
    /**
     * 시간대별 예약 분포
     */
    private List<HourlyCount> hourlyDistribution;

    /**
     * 요일별 예약 분포
     */
    private List<DayOfWeekCount> weekdayDistribution;

    /**
     * 피크 타임 (가장 바쁜 시간대)
     */
    private String peakHour;

    /**
     * 피크 타임 예약 수
     */
    private Integer peakHourCount;

    /**
     * 한산한 시간대
     */
    private String offPeakHour;

    /**
     * 한산한 시간대 예약 수
     */
    private Integer offPeakHourCount;
}
