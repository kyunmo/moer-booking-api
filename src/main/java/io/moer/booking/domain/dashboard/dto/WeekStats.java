package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class WeekStats {
    private Integer totalReservations;
    private Integer totalRevenue;
    private List<DailyCount> dailyCounts;
}