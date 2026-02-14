package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 월별 목표 달성률 응답
 */
@Getter
@Builder
@AllArgsConstructor
public class GoalStatsResponse {

    /** 조회 월 (yyyy-MM) */
    private String month;

    /** 매출 목표 */
    private Long revenueGoal;

    /** 현재 매출 */
    private Long currentRevenue;

    /** 매출 달성률 (%) */
    private Double revenueAchievementRate;

    /** 예약 목표 */
    private Integer reservationGoal;

    /** 현재 예약 수 */
    private Integer currentReservations;

    /** 예약 달성률 (%) */
    private Double reservationAchievementRate;

    /** 남은 일수 */
    private Integer daysRemaining;

    /** 예상 매출 (추세 기반) */
    private Long projectedRevenue;

    /** 예상 예약 수 (추세 기반) */
    private Integer projectedReservations;
}
