package io.moer.booking.domain.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 서비스 통계 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class ServiceStatisticsResponse {

    private Summary summary;
    private Comparison comparison;
    private List<ServiceRankingItem> serviceRankings;
    private List<CategoryDistributionItem> categoryDistribution;
    private List<ServiceTrendItem> serviceTrend;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Summary {
        private Integer totalServiceCount;
        private Integer uniqueServiceTypes;
        private Long averagePrice;
        private Integer categoryCount;
        private String mostPopularService;
        private String mostProfitableService;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Comparison {
        private Double totalServiceCountChange;
        private Double averagePriceChange;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ServiceRankingItem {
        private Integer rank;
        private Long serviceId;
        private String serviceName;
        private Long categoryId;
        private String categoryName;
        private Integer reservationCount;
        private Long totalRevenue;
        private Long averagePrice;
        private Double revenuePercentage;
        private Integer averageDuration;
        private Double completionRate;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CategoryDistributionItem {
        private Long categoryId;
        private String categoryName;
        private Integer serviceCount;
        private Integer reservationCount;
        private Long revenue;
        private Double percentage;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ServiceTrendItem {
        private Long serviceId;
        private String serviceName;
        private List<TrendItem> trend;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TrendItem {
        private String date;
        private Long revenue;
        private Integer count;
    }
}
