package io.moer.booking.domain.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 매출 통계 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class RevenueStatisticsResponse {

    private Summary summary;
    private Comparison comparison;
    private List<RevenueTrendItem> revenueTrend;
    private List<ServiceRevenueItem> revenueByService;
    private List<PaymentMethodItem> revenueByPaymentMethod;
    private GoalProgress goals;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Summary {
        private Long totalRevenue;
        private Long averageRevenue;
        private Long averageTransactionAmount;
        private Double completionRate;
        private Long customerLTV;
        private Integer averageServiceDuration;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Comparison {
        private Double revenueChange;
        private Double averageRevenueChange;
        private Double transactionAmountChange;
        private Double completionRateChange;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RevenueTrendItem {
        private String date;
        private Long revenue;
        private Integer reservationCount;
        private Integer completedCount;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ServiceRevenueItem {
        private Long serviceId;
        private String serviceName;
        private String categoryName;
        private Long revenue;
        private Double percentage;
        private Integer reservationCount;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PaymentMethodItem {
        private String method;
        private String methodName;
        private Long revenue;
        private Double percentage;
        private Integer count;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class GoalProgress {
        private Long revenueGoal;
        private Double revenueAchievementRate;
        private Long projectedRevenue;
        private Integer reservationGoal;
        private Double reservationAchievementRate;
        private Integer projectedReservations;
        private Integer daysElapsed;
        private Integer daysRemaining;
        private Integer totalDays;
    }
}
