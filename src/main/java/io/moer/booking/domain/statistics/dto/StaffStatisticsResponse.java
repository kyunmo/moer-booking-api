package io.moer.booking.domain.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 스태프 성과 통계 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class StaffStatisticsResponse {

    private List<StaffPerformanceItem> staffPerformances;
    private Comparison comparison;
    private List<StaffRevenueTrendItem> staffRevenueTrend;
    private List<StaffRadarItem> staffRadar;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class StaffPerformanceItem {
        private Long staffId;
        private String staffName;
        private String positionName;
        private String profileImageUrl;
        private Integer reservationCount;
        private Integer completedCount;
        private Integer cancelledCount;
        private Integer noShowCount;
        private Long totalRevenue;
        private Long averageRevenue;
        private Integer averageDuration;
        private Double completionRate;
        private Integer customerCount;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Comparison {
        private Double totalReservationsChange;
        private Double totalRevenueChange;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class StaffRevenueTrendItem {
        private Long staffId;
        private String staffName;
        private List<TrendItem> trend;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TrendItem {
        private String date;
        private Long revenue;
        private Integer reservationCount;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class StaffRadarItem {
        private Long staffId;
        private String staffName;
        private RadarMetrics metrics;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RadarMetrics {
        private Integer reservationVolume;
        private Integer revenue;
        private Integer completionRate;
        private Integer customerSatisfaction;
        private Integer efficiency;
    }
}
