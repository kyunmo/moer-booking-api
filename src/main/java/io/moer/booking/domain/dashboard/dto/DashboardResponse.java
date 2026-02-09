package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DashboardResponse {
    // 기본 통계
    private TodayStats todayStats;
    private WeekStats weekStats;
    private MonthStats monthStats;

    // 실시간 데이터
    private List<RecentReservation> recentReservations;
    private List<RecentCustomer> recentCustomers;

    // 신규 추가 - Phase 1
    private CancellationStats cancellationStats;
    private ActionAlerts actionAlerts;
    private CustomerSegments customerSegments;

    // 신규 추가 - Phase 2
    private List<StaffPerformance> topStaffPerformances;
    private List<ServiceStats> popularServices;
    private AverageMetrics averageMetrics;

    // 신규 추가 - Phase 3
    private RevenueTrend revenueTrend;
    private TimeSlotAnalysis timeSlotAnalysis;
    private GoalProgress goalProgress;

    // 신규 추가 - Trial System
    private TrialProgress trialProgress;
}