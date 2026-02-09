package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 매출 트렌드 분석
 */
@Getter
@Builder
@AllArgsConstructor
public class RevenueTrend {
    /**
     * 오늘 매출
     */
    private Integer todayRevenue;

    /**
     * 전일 매출
     */
    private Integer yesterdayRevenue;

    /**
     * 전일 대비 증감률 (%)
     */
    private Double dailyGrowthRate;

    /**
     * 이번 주 매출
     */
    private Integer thisWeekRevenue;

    /**
     * 전주 매출
     */
    private Integer lastWeekRevenue;

    /**
     * 전주 대비 증감률 (%)
     */
    private Double weeklyGrowthRate;

    /**
     * 이번 달 매출
     */
    private Integer thisMonthRevenue;

    /**
     * 전월 매출
     */
    private Integer lastMonthRevenue;

    /**
     * 전월 대비 증감률 (%)
     */
    private Double monthlyGrowthRate;

    /**
     * 올해 동월 매출
     */
    private Integer thisYearMonthRevenue;

    /**
     * 작년 동월 매출
     */
    private Integer lastYearMonthRevenue;

    /**
     * 전년 대비 증감률 (%)
     */
    private Double yearlyGrowthRate;

    /**
     * 최근 6개월 월별 매출 데이터
     */
    private List<MonthlyRevenue> monthlyRevenues;
}
