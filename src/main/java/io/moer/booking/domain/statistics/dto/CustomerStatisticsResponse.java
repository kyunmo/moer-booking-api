package io.moer.booking.domain.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 고객 통계 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class CustomerStatisticsResponse {

    private Summary summary;
    private Comparison comparison;
    private List<CustomerTrendItem> customerTrend;
    private List<SegmentItem> segments;
    private List<ReturningRateTrendItem> returningRateTrend;
    private List<LtvDistributionItem> ltvDistribution;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Summary {
        private Integer totalCustomers;
        private Integer newCustomers;
        private Double returningRate;
        private Double averageVisitCount;
        private Long averageLTV;
        private Double churnRate;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Comparison {
        private Double totalCustomersChange;
        private Double newCustomersChange;
        private Double returningRateChange;
        private Double averageVisitCountChange;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CustomerTrendItem {
        private String date;
        private Integer newCustomers;
        private Integer returningCustomers;
        private Integer totalActive;
        private Integer churned;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SegmentItem {
        private String segment;
        private String segmentName;
        private String description;
        private Integer count;
        private Double percentage;
        private Long totalRevenue;
        private Long averageRevenue;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ReturningRateTrendItem {
        private String date;
        private Double rate;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class LtvDistributionItem {
        private String range;
        private Long min;
        private Long max;
        private Integer count;
    }
}
