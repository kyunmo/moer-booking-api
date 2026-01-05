package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DashboardResponse {
    private TodayStats todayStats;
    private WeekStats weekStats;
    private MonthStats monthStats;
    private List<RecentReservation> recentReservations;
    private List<RecentCustomer> recentCustomers;
}