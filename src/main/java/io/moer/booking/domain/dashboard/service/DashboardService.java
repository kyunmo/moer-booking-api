package io.moer.booking.domain.dashboard.service;

import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.customer.repository.CustomerRepository;
import io.moer.booking.domain.dashboard.dto.*;
import io.moer.booking.domain.reservation.ReservationStatus;
import io.moer.booking.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final BusinessRepository businessRepository;
    private final ReservationRepository reservationRepository;
    private final CustomerRepository customerRepository;

    public DashboardResponse getDashboardStats(Long businessId, LocalDate date) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        // 오늘 통계
        TodayStats todayStats = getTodayStats(businessId, date);

        // 이번 주 통계
        WeekStats weekStats = getWeekStats(businessId, date);

        // 이번 달 통계
        MonthStats monthStats = getMonthStats(businessId, date);

        // 최근 예약 목록
        List<RecentReservation> recentReservations = getRecentReservations(businessId, date);

        // 최근 신규 고객
        List<RecentCustomer> recentCustomers = getRecentCustomers(businessId);

        return DashboardResponse.builder()
                .todayStats(todayStats)
                .weekStats(weekStats)
                .monthStats(monthStats)
                .recentReservations(recentReservations)
                .recentCustomers(recentCustomers)
                .build();
    }

    private TodayStats getTodayStats(Long businessId, LocalDate date) {
        int totalCount = reservationRepository.countByBusinessIdAndDate(businessId, date);
        int pendingCount = reservationRepository.countByBusinessIdAndDateAndStatus(
                businessId, date, ReservationStatus.PENDING);
        int confirmedCount = reservationRepository.countByBusinessIdAndDateAndStatus(
                businessId, date, ReservationStatus.CONFIRMED);
        int completedCount = reservationRepository.countByBusinessIdAndDateAndStatus(
                businessId, date, ReservationStatus.COMPLETED);

        int expectedRevenue = reservationRepository.sumTotalPriceByBusinessIdAndDateAndStatusIn(
                businessId, date, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));

        return TodayStats.builder()
                .totalReservations(totalCount)
                .pendingReservations(pendingCount)
                .confirmedReservations(confirmedCount)
                .completedReservations(completedCount)
                .expectedRevenue(expectedRevenue)
                .build();
    }

    private WeekStats getWeekStats(Long businessId, LocalDate date) {
        LocalDate weekStart = date.minusDays(date.getDayOfWeek().getValue() - 1);
        LocalDate weekEnd = weekStart.plusDays(6);

        int totalCount = reservationRepository.countByBusinessIdAndDateRange(
                businessId, weekStart, weekEnd);

        int totalRevenue = reservationRepository.sumTotalPriceByBusinessIdAndDateRangeAndStatus(
                businessId, weekStart, weekEnd, ReservationStatus.COMPLETED);

        List<DailyCount> dailyCounts = reservationRepository.countByBusinessIdAndDateRangeGroupByDate(
                businessId, weekStart, weekEnd);

        return WeekStats.builder()
                .totalReservations(totalCount)
                .totalRevenue(totalRevenue)
                .dailyCounts(dailyCounts)
                .build();
    }

    private MonthStats getMonthStats(Long businessId, LocalDate date) {
        YearMonth yearMonth = YearMonth.from(date);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        int totalCount = reservationRepository.countByBusinessIdAndDateRange(
                businessId, monthStart, monthEnd);

        int totalRevenue = reservationRepository.sumTotalPriceByBusinessIdAndDateRangeAndStatus(
                businessId, monthStart, monthEnd, ReservationStatus.COMPLETED);

        int newCustomersCount = customerRepository.countByBusinessIdAndCreatedAtBetween(
                businessId, monthStart.atStartOfDay(), monthEnd.atTime(23, 59, 59));

        return MonthStats.builder()
                .totalReservations(totalCount)
                .totalRevenue(totalRevenue)
                .newCustomers(newCustomersCount)
                .build();
    }

    private List<RecentReservation> getRecentReservations(Long businessId, LocalDate date) {
        return reservationRepository.findRecentByBusinessIdAndDate(businessId, date, 10);
    }

    private List<RecentCustomer> getRecentCustomers(Long businessId) {
        return customerRepository.findRecentByBusinessId(businessId, 5);
    }
}