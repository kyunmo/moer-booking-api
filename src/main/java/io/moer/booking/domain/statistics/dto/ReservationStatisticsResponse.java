package io.moer.booking.domain.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 예약 통계 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class ReservationStatisticsResponse {

    private Summary summary;
    private Comparison comparison;
    private List<ReservationTrendItem> reservationTrend;
    private List<HourlyHeatmapItem> hourlyHeatmap;
    private List<StatusDistributionItem> statusDistribution;
    private List<DailyDistributionItem> dailyDistribution;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Summary {
        private Integer totalReservations;
        private Integer completedReservations;
        private Integer cancelledReservations;
        private Integer noShowReservations;
        private Integer pendingReservations;
        private Double completionRate;
        private Double cancellationRate;
        private Double noShowRate;
        private Long lostRevenue;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Comparison {
        private Double totalChange;
        private Double completedChange;
        private Double cancelledChange;
        private Double noShowChange;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ReservationTrendItem {
        private String date;
        private Integer total;
        private Integer completed;
        private Integer cancelled;
        private Integer noShow;
        private Integer pending;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class HourlyHeatmapItem {
        private Integer dayOfWeek;
        private String dayName;
        private List<HourCount> hours;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class HourCount {
        private Integer hour;
        private Integer count;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class StatusDistributionItem {
        private String status;
        private String statusName;
        private Integer count;
        private Double percentage;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DailyDistributionItem {
        private Integer dayOfWeek;
        private String dayName;
        private Double averageCount;
        private Integer totalCount;
    }
}
