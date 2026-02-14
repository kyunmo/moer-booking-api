package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 기간별 통계 응답
 */
@Getter
@Builder
@AllArgsConstructor
public class PeriodStatsResponse {

    private Period period;
    private PeriodStats stats;
    private PeriodComparison comparison;
    private List<DailyBreakdown> dailyBreakdown;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Period {
        private LocalDate start;
        private LocalDate end;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PeriodStats {
        private Integer totalReservations;
        private Integer completedReservations;
        private Integer cancelledReservations;
        private Integer noShowReservations;
        private Long totalRevenue;
        private Long averageRevenuePerReservation;
        private Integer newCustomers;
        private Integer returningCustomers;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PeriodComparison {
        private Period period;
        private Double reservationsChange;
        private Double revenueChange;
        private Double newCustomersChange;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DailyBreakdown {
        private LocalDate date;
        private Integer reservations;
        private Long revenue;
    }
}
