package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 목표 달성률
 */
@Getter
@Builder
@AllArgsConstructor
public class GoalProgress {
    /**
     * 일일 매출 목표
     */
    private Integer dailyRevenueGoal;

    /**
     * 오늘 매출
     */
    private Integer todayRevenue;

    /**
     * 일일 매출 달성률 (%)
     */
    private Double dailyRevenueAchievement;

    /**
     * 월간 매출 목표
     */
    private Integer monthlyRevenueGoal;

    /**
     * 이번 달 매출
     */
    private Integer thisMonthRevenue;

    /**
     * 월간 매출 달성률 (%)
     */
    private Double monthlyRevenueAchievement;

    /**
     * 월간 신규 고객 목표
     */
    private Integer monthlyNewCustomerGoal;

    /**
     * 이번 달 신규 고객 수
     */
    private Integer thisMonthNewCustomers;

    /**
     * 월간 신규 고객 달성률 (%)
     */
    private Double monthlyNewCustomerAchievement;
}
