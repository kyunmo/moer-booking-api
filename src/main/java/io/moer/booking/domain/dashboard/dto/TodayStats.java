package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TodayStats {
    private Integer totalReservations;
    private Integer pendingReservations;
    private Integer confirmedReservations;
    private Integer completedReservations;
    private Integer expectedRevenue;
}