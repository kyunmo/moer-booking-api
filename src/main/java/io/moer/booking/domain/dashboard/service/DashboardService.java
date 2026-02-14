package io.moer.booking.domain.dashboard.service;

import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.customer.repository.CustomerRepository;
import io.moer.booking.domain.dashboard.dto.*;
import io.moer.booking.domain.reservation.ReservationStatus;
import io.moer.booking.domain.reservation.repository.ReservationRepository;
import io.moer.booking.domain.user.User;
import io.moer.booking.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final BusinessRepository businessRepository;
    private final ReservationRepository reservationRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public DashboardResponse getDashboardStats(Long businessId, LocalDate date) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        // 기본 통계
        TodayStats todayStats = getTodayStats(businessId, date);
        WeekStats weekStats = getWeekStats(businessId, date);
        MonthStats monthStats = getMonthStats(businessId, date);

        // 실시간 데이터
        List<RecentReservation> recentReservations = getRecentReservations(businessId, date);
        List<RecentCustomer> recentCustomers = getRecentCustomers(businessId);

        // Phase 1: 즉시 유용한 통계
        CancellationStats cancellationStats = getCancellationStats(businessId, date);
        ActionAlerts actionAlerts = getActionAlerts(businessId, date);
        CustomerSegments customerSegments = getCustomerSegments(businessId);

        // Phase 2: 핵심 분석
        List<StaffPerformance> topStaffPerformances = getTopStaffPerformances(businessId, date);
        List<ServiceStats> popularServices = getPopularServices(businessId, date);
        AverageMetrics averageMetrics = getAverageMetrics(businessId);

        // Phase 3: 고급 인사이트
        RevenueTrend revenueTrend = getRevenueTrend(businessId, date);
        TimeSlotAnalysis timeSlotAnalysis = getTimeSlotAnalysis(businessId, date);
        GoalProgress goalProgress = getGoalProgress(businessId, date);

        // Trial System: 체험판 진행 상황
        TrialProgress trialProgress = getTrialProgress(businessId);

        return DashboardResponse.builder()
                .todayStats(todayStats)
                .weekStats(weekStats)
                .monthStats(monthStats)
                .recentReservations(recentReservations)
                .recentCustomers(recentCustomers)
                .cancellationStats(cancellationStats)
                .actionAlerts(actionAlerts)
                .customerSegments(customerSegments)
                .topStaffPerformances(topStaffPerformances)
                .popularServices(popularServices)
                .averageMetrics(averageMetrics)
                .revenueTrend(revenueTrend)
                .timeSlotAnalysis(timeSlotAnalysis)
                .goalProgress(goalProgress)
                .trialProgress(trialProgress)
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

    // ========================================
    // Phase 1: 취소/노쇼 통계
    // ========================================

    private CancellationStats getCancellationStats(Long businessId, LocalDate date) {
        YearMonth yearMonth = YearMonth.from(date);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        // 이번 달 취소 건수
        int cancelledCount = reservationRepository.countByBusinessIdAndDateRangeAndStatus(
                businessId, monthStart, monthEnd, ReservationStatus.CANCELLED);

        // 이번 달 노쇼 건수
        int noShowCount = reservationRepository.countByBusinessIdAndDateRangeAndStatus(
                businessId, monthStart, monthEnd, ReservationStatus.NO_SHOW);

        // 이번 달 전체 예약 수
        int totalCount = reservationRepository.countByBusinessIdAndDateRange(
                businessId, monthStart, monthEnd);

        // 취소율 및 노쇼율 계산
        double cancellationRate = totalCount > 0 ? (cancelledCount * 100.0 / totalCount) : 0.0;
        double noShowRate = totalCount > 0 ? (noShowCount * 100.0 / totalCount) : 0.0;

        // 취소로 인한 매출 손실액
        Integer lostRevenue = reservationRepository.sumTotalPriceByBusinessIdAndDateRangeAndStatusIn(
                businessId, monthStart, monthEnd, List.of(ReservationStatus.CANCELLED, ReservationStatus.NO_SHOW));

        return CancellationStats.builder()
                .cancelledCount(cancelledCount)
                .noShowCount(noShowCount)
                .cancellationRate(Math.round(cancellationRate * 100.0) / 100.0)
                .noShowRate(Math.round(noShowRate * 100.0) / 100.0)
                .lostRevenue(lostRevenue != null ? lostRevenue : 0)
                .build();
    }

    // ========================================
    // Phase 1: 실시간 액션 알림
    // ========================================

    private ActionAlerts getActionAlerts(Long businessId, LocalDate date) {
        // 확정 대기중 예약 수
        int pendingReservations = reservationRepository.countByBusinessIdAndDateAndStatus(
                businessId, date, ReservationStatus.PENDING);

        // 1시간 이내 시작 예약 수
        java.time.LocalTime now = java.time.LocalTime.now();
        java.time.LocalTime oneHourLater = now.plusHours(1);
        int upcomingReservations = reservationRepository.countUpcomingReservations(
                businessId, date, now, oneHourLater);

        // 오늘 생일 고객 수
        int birthdayCustomers = customerRepository.countBirthdayCustomers(
                businessId, date.getMonthValue(), date.getDayOfMonth());

        // 재방문 유도 대상 고객 수 (1개월 이상 미방문)
        int inactiveCustomers = customerRepository.countInactiveCustomers(businessId, 1);

        return ActionAlerts.builder()
                .pendingReservations(pendingReservations)
                .upcomingReservations(upcomingReservations)
                .birthdayCustomers(birthdayCustomers)
                .inactiveCustomers(inactiveCustomers)
                .build();
    }

    // ========================================
    // Phase 1: 고객 세그먼트 분석
    // ========================================

    private CustomerSegments getCustomerSegments(Long businessId) {
        // VIP 고객 (10회 이상)
        int vipCount = customerRepository.countByBusinessIdAndVisitCountBetween(
                businessId, 10, Integer.MAX_VALUE);

        // 단골 고객 (3~9회)
        int regularCount = customerRepository.countByBusinessIdAndVisitCountBetween(
                businessId, 3, 9);

        // 신규 고객 (1회)
        int newCount = customerRepository.countByBusinessIdAndVisitCountBetween(
                businessId, 1, 1);

        // 이탈 고객 (3개월 이상 미방문)
        int inactiveCount = customerRepository.countInactiveCustomersByLastVisit(
                businessId, 3);

        // 전체 고객 수
        long totalCustomers = customerRepository.countByBusinessId(businessId);

        // 재방문 고객 수 (2회 이상)
        int returningCustomers = customerRepository.countReturningCustomers(businessId);

        // 재방문율 계산
        double returningRate = totalCustomers > 0
                ? (returningCustomers * 100.0 / totalCustomers)
                : 0.0;

        return CustomerSegments.builder()
                .vipCount(vipCount)
                .regularCount(regularCount)
                .newCount(newCount)
                .inactiveCount(inactiveCount)
                .totalCustomers((int) totalCustomers)
                .returningRate(Math.round(returningRate * 100.0) / 100.0)
                .build();
    }

    // ========================================
    // Phase 2: 직원별 성과 분석
    // ========================================

    private List<StaffPerformance> getTopStaffPerformances(Long businessId, LocalDate date) {
        YearMonth yearMonth = YearMonth.from(date);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        return reservationRepository.getStaffPerformance(businessId, monthStart, monthEnd, 3);
    }

    // ========================================
    // Phase 2: 인기 서비스 TOP 5
    // ========================================

    private List<ServiceStats> getPopularServices(Long businessId, LocalDate date) {
        YearMonth yearMonth = YearMonth.from(date);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        return reservationRepository.getPopularServices(businessId, monthStart, monthEnd, 5);
    }

    // ========================================
    // Phase 2: 평균 지표
    // ========================================

    private AverageMetrics getAverageMetrics(Long businessId) {
        // 평균 예약 금액 (완료된 예약 기준)
        Integer avgReservationAmount = reservationRepository.getAverageReservationAmount(
                businessId, ReservationStatus.COMPLETED);

        // 평균 서비스 시간
        Integer avgServiceDuration = reservationRepository.getAverageServiceDuration(
                businessId, ReservationStatus.COMPLETED);

        // 고객당 평균 방문 횟수
        Double avgVisitCount = customerRepository.getAverageVisitCount(businessId);

        // 고객당 평균 결제액 (LTV)
        Integer avgCustomerLTV = customerRepository.getAverageCustomerLifetimeValue(businessId);

        // 예약 → 완료 전환율
        int totalReservations = reservationRepository.countByBusinessIdAndDateRange(
                businessId, LocalDate.now().minusMonths(1), LocalDate.now());
        int completedReservations = reservationRepository.countByBusinessIdAndDateRangeAndStatus(
                businessId, LocalDate.now().minusMonths(1), LocalDate.now(), ReservationStatus.COMPLETED);

        double completionRate = totalReservations > 0
                ? (completedReservations * 100.0 / totalReservations)
                : 0.0;

        return AverageMetrics.builder()
                .averageReservationAmount(avgReservationAmount != null ? avgReservationAmount : 0)
                .averageServiceDuration(avgServiceDuration != null ? avgServiceDuration : 0)
                .averageVisitCount(avgVisitCount != null ? Math.round(avgVisitCount * 100.0) / 100.0 : 0.0)
                .averageCustomerLifetimeValue(avgCustomerLTV != null ? avgCustomerLTV : 0)
                .completionRate(Math.round(completionRate * 100.0) / 100.0)
                .build();
    }

    // ========================================
    // Phase 3: 매출 트렌드 분석
    // ========================================

    private RevenueTrend getRevenueTrend(Long businessId, LocalDate date) {
        // 오늘 매출
        Integer todayRevenue = reservationRepository.getRevenueByDate(businessId, date);

        // 전일 매출
        LocalDate yesterday = date.minusDays(1);
        Integer yesterdayRevenue = reservationRepository.getRevenueByDate(businessId, yesterday);

        // 전일 대비 증감률
        double dailyGrowthRate = calculateGrowthRate(yesterdayRevenue, todayRevenue);

        // 이번 주 매출
        LocalDate weekStart = date.minusDays(date.getDayOfWeek().getValue() - 1);
        LocalDate weekEnd = weekStart.plusDays(6);
        Integer thisWeekRevenue = reservationRepository.getRevenueByDateRange(businessId, weekStart, weekEnd);

        // 전주 매출
        LocalDate lastWeekStart = weekStart.minusWeeks(1);
        LocalDate lastWeekEnd = lastWeekStart.plusDays(6);
        Integer lastWeekRevenue = reservationRepository.getRevenueByDateRange(businessId, lastWeekStart, lastWeekEnd);

        // 전주 대비 증감률
        double weeklyGrowthRate = calculateGrowthRate(lastWeekRevenue, thisWeekRevenue);

        // 이번 달 매출
        YearMonth thisYearMonth = YearMonth.from(date);
        LocalDate monthStart = thisYearMonth.atDay(1);
        LocalDate monthEnd = thisYearMonth.atEndOfMonth();
        Integer thisMonthRevenue = reservationRepository.getRevenueByDateRange(businessId, monthStart, monthEnd);

        // 전월 매출
        YearMonth lastYearMonth = thisYearMonth.minusMonths(1);
        LocalDate lastMonthStart = lastYearMonth.atDay(1);
        LocalDate lastMonthEnd = lastYearMonth.atEndOfMonth();
        Integer lastMonthRevenue = reservationRepository.getRevenueByDateRange(businessId, lastMonthStart, lastMonthEnd);

        // 전월 대비 증감률
        double monthlyGrowthRate = calculateGrowthRate(lastMonthRevenue, thisMonthRevenue);

        // 작년 동월 매출
        YearMonth lastYearSameMonth = thisYearMonth.minusYears(1);
        LocalDate lastYearMonthStart = lastYearSameMonth.atDay(1);
        LocalDate lastYearMonthEnd = lastYearSameMonth.atEndOfMonth();
        Integer lastYearMonthRevenue = reservationRepository.getRevenueByDateRange(businessId, lastYearMonthStart, lastYearMonthEnd);

        // 전년 대비 증감률
        double yearlyGrowthRate = calculateGrowthRate(lastYearMonthRevenue, thisMonthRevenue);

        // 최근 6개월 월별 매출
        LocalDate sixMonthsAgo = date.minusMonths(5).withDayOfMonth(1);
        List<MonthlyRevenue> monthlyRevenues = reservationRepository.getMonthlyRevenues(
                businessId, sixMonthsAgo, monthEnd);

        return RevenueTrend.builder()
                .todayRevenue(todayRevenue != null ? todayRevenue : 0)
                .yesterdayRevenue(yesterdayRevenue != null ? yesterdayRevenue : 0)
                .dailyGrowthRate(dailyGrowthRate)
                .thisWeekRevenue(thisWeekRevenue != null ? thisWeekRevenue : 0)
                .lastWeekRevenue(lastWeekRevenue != null ? lastWeekRevenue : 0)
                .weeklyGrowthRate(weeklyGrowthRate)
                .thisMonthRevenue(thisMonthRevenue != null ? thisMonthRevenue : 0)
                .lastMonthRevenue(lastMonthRevenue != null ? lastMonthRevenue : 0)
                .monthlyGrowthRate(monthlyGrowthRate)
                .thisYearMonthRevenue(thisMonthRevenue != null ? thisMonthRevenue : 0)
                .lastYearMonthRevenue(lastYearMonthRevenue != null ? lastYearMonthRevenue : 0)
                .yearlyGrowthRate(yearlyGrowthRate)
                .monthlyRevenues(monthlyRevenues)
                .build();
    }

    // ========================================
    // Phase 3: 시간대별 분석
    // ========================================

    private TimeSlotAnalysis getTimeSlotAnalysis(Long businessId, LocalDate date) {
        YearMonth yearMonth = YearMonth.from(date);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        // 시간대별 예약 분포
        List<HourlyCount> hourlyDistribution = reservationRepository.getHourlyDistribution(
                businessId, monthStart, monthEnd);

        // 요일별 예약 분포
        List<DayOfWeekCount> weekdayDistribution = reservationRepository.getWeekdayDistribution(
                businessId, monthStart, monthEnd);

        // 피크 타임 및 한산한 시간대 찾기
        String peakHour = null;
        Integer peakHourCount = 0;
        String offPeakHour = null;
        Integer offPeakHourCount = Integer.MAX_VALUE;

        if (!hourlyDistribution.isEmpty()) {
            for (HourlyCount hourly : hourlyDistribution) {
                if (hourly.getCount() > peakHourCount) {
                    peakHour = hourly.getHour();
                    peakHourCount = hourly.getCount();
                }
                if (hourly.getCount() < offPeakHourCount && hourly.getCount() > 0) {
                    offPeakHour = hourly.getHour();
                    offPeakHourCount = hourly.getCount();
                }
            }
        }

        return TimeSlotAnalysis.builder()
                .hourlyDistribution(hourlyDistribution)
                .weekdayDistribution(weekdayDistribution)
                .peakHour(peakHour)
                .peakHourCount(peakHourCount)
                .offPeakHour(offPeakHour)
                .offPeakHourCount(offPeakHourCount != Integer.MAX_VALUE ? offPeakHourCount : 0)
                .build();
    }

    // ========================================
    // Phase 3: 목표 달성률
    // ========================================

    private GoalProgress getGoalProgress(Long businessId, LocalDate date) {
        // Business 정보 조회 (목표 값)
        io.moer.booking.domain.business.Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        // 오늘 매출
        Integer todayRevenue = reservationRepository.getRevenueByDate(businessId, date);

        // 이번 달 매출
        YearMonth yearMonth = YearMonth.from(date);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        Integer thisMonthRevenue = reservationRepository.getRevenueByDateRange(businessId, monthStart, monthEnd);

        // 이번 달 신규 고객 수
        int thisMonthNewCustomers = customerRepository.countByBusinessIdAndCreatedAtBetween(
                businessId, monthStart.atStartOfDay(), monthEnd.atTime(23, 59, 59));

        // 달성률 계산
        Integer dailyGoal = business.getDailyRevenueGoal();
        Double dailyAchievement = dailyGoal != null && dailyGoal > 0
                ? (todayRevenue * 100.0 / dailyGoal)
                : null;

        Integer monthlyGoal = business.getMonthlyRevenueGoal();
        Double monthlyAchievement = monthlyGoal != null && monthlyGoal > 0
                ? (thisMonthRevenue * 100.0 / monthlyGoal)
                : null;

        Integer customerGoal = business.getMonthlyNewCustomerGoal();
        Double customerAchievement = customerGoal != null && customerGoal > 0
                ? (thisMonthNewCustomers * 100.0 / customerGoal)
                : null;

        return GoalProgress.builder()
                .dailyRevenueGoal(dailyGoal)
                .todayRevenue(todayRevenue != null ? todayRevenue : 0)
                .dailyRevenueAchievement(dailyAchievement != null ? Math.round(dailyAchievement * 100.0) / 100.0 : null)
                .monthlyRevenueGoal(monthlyGoal)
                .thisMonthRevenue(thisMonthRevenue != null ? thisMonthRevenue : 0)
                .monthlyRevenueAchievement(monthlyAchievement != null ? Math.round(monthlyAchievement * 100.0) / 100.0 : null)
                .monthlyNewCustomerGoal(customerGoal)
                .thisMonthNewCustomers(thisMonthNewCustomers)
                .monthlyNewCustomerAchievement(customerAchievement != null ? Math.round(customerAchievement * 100.0) / 100.0 : null)
                .build();
    }

    // ========================================
    // Trial System: 체험판 진행 상황
    // ========================================

    private TrialProgress getTrialProgress(Long businessId) {
        // Business 정보로 Owner 조회
        io.moer.booking.domain.business.Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        User owner = userRepository.findById(business.getOwnerId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        return TrialProgress.from(owner);
    }

    // ========================================
    // Phase 2: 기간별 통계
    // ========================================

    public PeriodStatsResponse getPeriodStats(Long businessId, LocalDate startDate, LocalDate endDate, String compareWith) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        // 현재 기간 통계
        PeriodStatsResponse.PeriodStats currentStats = calculatePeriodStats(businessId, startDate, endDate);

        // 일별 분석
        List<PeriodStatsResponse.DailyBreakdown> dailyBreakdown = buildDailyBreakdown(businessId, startDate, endDate);

        // 비교 기간 (선택)
        PeriodStatsResponse.PeriodComparison comparison = null;
        if ("PREVIOUS_PERIOD".equals(compareWith)) {
            long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            LocalDate compEnd = startDate.minusDays(1);
            LocalDate compStart = compEnd.minusDays(days - 1);
            comparison = buildComparison(businessId, compStart, compEnd, currentStats);
        } else if ("LAST_YEAR".equals(compareWith)) {
            LocalDate compStart = startDate.minusYears(1);
            LocalDate compEnd = endDate.minusYears(1);
            comparison = buildComparison(businessId, compStart, compEnd, currentStats);
        }

        return PeriodStatsResponse.builder()
                .period(PeriodStatsResponse.Period.builder().start(startDate).end(endDate).build())
                .stats(currentStats)
                .comparison(comparison)
                .dailyBreakdown(dailyBreakdown)
                .build();
    }

    private PeriodStatsResponse.PeriodStats calculatePeriodStats(Long businessId, LocalDate start, LocalDate end) {
        int total = reservationRepository.countByBusinessIdAndDateRange(businessId, start, end);
        int completed = reservationRepository.countByBusinessIdAndDateRangeAndStatus(businessId, start, end, ReservationStatus.COMPLETED);
        int cancelled = reservationRepository.countByBusinessIdAndDateRangeAndStatus(businessId, start, end, ReservationStatus.CANCELLED);
        int noShow = reservationRepository.countByBusinessIdAndDateRangeAndStatus(businessId, start, end, ReservationStatus.NO_SHOW);
        long revenue = reservationRepository.sumTotalPriceByBusinessIdAndDateRangeAndStatus(businessId, start, end, ReservationStatus.COMPLETED);
        int newCust = customerRepository.countByBusinessIdAndCreatedAtBetween(businessId, start.atStartOfDay(), end.atTime(23, 59, 59));
        int returning = customerRepository.countReturningCustomers(businessId);

        return PeriodStatsResponse.PeriodStats.builder()
                .totalReservations(total)
                .completedReservations(completed)
                .cancelledReservations(cancelled)
                .noShowReservations(noShow)
                .totalRevenue(revenue)
                .averageRevenuePerReservation(completed > 0 ? revenue / completed : 0L)
                .newCustomers(newCust)
                .returningCustomers(returning)
                .build();
    }

    private List<PeriodStatsResponse.DailyBreakdown> buildDailyBreakdown(Long businessId, LocalDate start, LocalDate end) {
        List<Map<String, Object>> rows = reservationRepository.getRevenueByDateRangeGroupByDate(businessId, start, end);
        List<PeriodStatsResponse.DailyBreakdown> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(PeriodStatsResponse.DailyBreakdown.builder()
                    .date(row.get("date") instanceof java.sql.Date d ? d.toLocalDate() : LocalDate.parse(row.get("date").toString()))
                    .reservations(((Number) row.get("reservations")).intValue())
                    .revenue(((Number) row.get("revenue")).longValue())
                    .build());
        }
        return result;
    }

    private PeriodStatsResponse.PeriodComparison buildComparison(Long businessId, LocalDate compStart, LocalDate compEnd,
                                                                   PeriodStatsResponse.PeriodStats current) {
        PeriodStatsResponse.PeriodStats prev = calculatePeriodStats(businessId, compStart, compEnd);

        return PeriodStatsResponse.PeriodComparison.builder()
                .period(PeriodStatsResponse.Period.builder().start(compStart).end(compEnd).build())
                .reservationsChange(calcPctChange(prev.getTotalReservations(), current.getTotalReservations()))
                .revenueChange(calcPctChange(prev.getTotalRevenue(), current.getTotalRevenue()))
                .newCustomersChange(calcPctChange(prev.getNewCustomers(), current.getNewCustomers()))
                .build();
    }

    private Double calcPctChange(Number prev, Number cur) {
        if (prev == null || prev.doubleValue() == 0) return cur != null && cur.doubleValue() > 0 ? 100.0 : 0.0;
        double rate = ((cur.doubleValue() - prev.doubleValue()) / prev.doubleValue()) * 100;
        return Math.round(rate * 10.0) / 10.0;
    }

    // ========================================
    // Phase 2: 목표 달성률
    // ========================================

    public GoalStatsResponse getGoalStats(Long businessId, YearMonth month) {
        io.moer.booking.domain.business.Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        LocalDate today = LocalDate.now();
        LocalDate effectiveEnd = today.isBefore(monthEnd) ? today : monthEnd;

        int currentReservations = reservationRepository.countByBusinessIdAndDateRange(businessId, monthStart, monthEnd);
        long currentRevenue = reservationRepository.sumTotalPriceByBusinessIdAndDateRangeAndStatus(
                businessId, monthStart, effectiveEnd, ReservationStatus.COMPLETED);

        Integer revenueGoalInt = business.getMonthlyRevenueGoal();
        Long revenueGoal = revenueGoalInt != null ? revenueGoalInt.longValue() : null;

        int totalDays = month.lengthOfMonth();
        int daysRemaining = today.isBefore(monthEnd) ? (int) ChronoUnit.DAYS.between(today, monthEnd) : 0;
        int elapsedDays = totalDays - daysRemaining;

        Long projectedRevenue = elapsedDays > 0 ? (currentRevenue * totalDays / elapsedDays) : 0L;
        Integer projectedReservations = elapsedDays > 0 ? (currentReservations * totalDays / elapsedDays) : 0;

        return GoalStatsResponse.builder()
                .month(month.toString())
                .revenueGoal(revenueGoal)
                .currentRevenue(currentRevenue)
                .revenueAchievementRate(revenueGoal != null && revenueGoal > 0
                        ? Math.round((currentRevenue * 1000.0 / revenueGoal)) / 10.0 : null)
                .currentReservations(currentReservations)
                .daysRemaining(daysRemaining)
                .projectedRevenue(projectedRevenue)
                .projectedReservations(projectedReservations)
                .build();
    }

    // ========================================
    // 헬퍼 메서드
    // ========================================

    /**
     * 증감률 계산
     */
    private double calculateGrowthRate(Integer previous, Integer current) {
        if (previous == null || previous == 0) {
            return current != null && current > 0 ? 100.0 : 0.0;
        }
        if (current == null) {
            return -100.0;
        }
        double rate = ((current - previous) * 100.0 / previous);
        return Math.round(rate * 100.0) / 100.0;
    }
}