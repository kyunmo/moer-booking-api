package io.moer.booking.domain.statistics.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.staff.repository.StaffRepository;
import io.moer.booking.domain.statistics.dto.*;
import io.moer.booking.domain.statistics.repository.StatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final StatisticsRepository statisticsRepository;
    private final BusinessRepository businessRepository;
    private final StaffRepository staffRepository;

    // ========================================
    // 1. Revenue Statistics (매출 통계)
    // ========================================

    public RevenueStatisticsResponse getRevenueStatistics(Long businessId, StatisticsSearchCondition condition) {
        validateSearchCondition(condition);
        validateBusinessExists(businessId);

        LocalDate startDate = condition.getStartDate();
        LocalDate endDate = condition.getEndDate();
        String groupBy = condition.getGroupBy() != null ? condition.getGroupBy() : "daily";

        // 1. Summary
        Map<String, Object> summaryMap = statisticsRepository.getRevenueSummary(businessId, startDate, endDate);
        RevenueStatisticsResponse.Summary summary = buildRevenueSummary(summaryMap, startDate, endDate);

        // 2. Revenue trend
        List<Map<String, Object>> trendRows = statisticsRepository.getRevenueTrend(businessId, startDate, endDate, groupBy);
        List<RevenueStatisticsResponse.RevenueTrendItem> revenueTrend = trendRows.stream()
                .map(row -> RevenueStatisticsResponse.RevenueTrendItem.builder()
                        .date(toStr(row.get("date")))
                        .revenue(toLong(row.get("revenue")))
                        .reservationCount(toInt(row.get("reservationCount")))
                        .completedCount(toInt(row.get("completedCount")))
                        .build())
                .toList();

        // 3. Revenue by service
        List<Map<String, Object>> serviceRows = statisticsRepository.getRevenueByService(businessId, startDate, endDate);
        List<RevenueStatisticsResponse.ServiceRevenueItem> revenueByService = serviceRows.stream()
                .map(row -> RevenueStatisticsResponse.ServiceRevenueItem.builder()
                        .serviceId(toLong(row.get("serviceId")))
                        .serviceName(toStr(row.get("serviceName")))
                        .categoryName(toStr(row.get("categoryName")))
                        .revenue(toLong(row.get("revenue")))
                        .percentage(toDouble(row.get("percentage")))
                        .reservationCount(toInt(row.get("reservationCount")))
                        .build())
                .toList();

        // 4. Revenue by payment method
        List<Map<String, Object>> paymentRows = statisticsRepository.getRevenueByPaymentMethod(businessId, startDate, endDate);
        List<RevenueStatisticsResponse.PaymentMethodItem> revenueByPaymentMethod = paymentRows.stream()
                .map(row -> RevenueStatisticsResponse.PaymentMethodItem.builder()
                        .method(toStr(row.get("method")))
                        .methodName(toStr(row.get("methodName")))
                        .revenue(toLong(row.get("revenue")))
                        .percentage(toDouble(row.get("percentage")))
                        .count(toInt(row.get("count")))
                        .build())
                .toList();

        // 5. Goal progress
        RevenueStatisticsResponse.GoalProgress goals = buildGoalProgress(businessId, startDate, endDate, summaryMap);

        // 6. Comparison (optional)
        RevenueStatisticsResponse.Comparison comparison = null;
        if (condition.getCompareWith() != null) {
            LocalDate[] compDates = getComparisonDates(startDate, endDate, condition.getCompareWith());
            if (compDates != null) {
                Map<String, Object> compSummary = statisticsRepository.getRevenueSummary(
                        businessId, compDates[0], compDates[1]);
                long compTotalRev = toLong(compSummary.get("totalRevenue"));
                long compDays = ChronoUnit.DAYS.between(compDates[0], compDates[1]) + 1;
                long compAvgRev = compDays > 0 ? compTotalRev / compDays : 0;
                long curTotalRev = toLong(summaryMap.get("totalRevenue"));
                long curDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
                long curAvgRev = curDays > 0 ? curTotalRev / curDays : 0;
                comparison = RevenueStatisticsResponse.Comparison.builder()
                        .revenueChange(calcPctChange(compSummary.get("totalRevenue"), summaryMap.get("totalRevenue")))
                        .averageRevenueChange(calcPctChange(compAvgRev, curAvgRev))
                        .transactionAmountChange(calcPctChange(compSummary.get("averageTransactionAmount"), summaryMap.get("averageTransactionAmount")))
                        .completionRateChange(calcPctChange(compSummary.get("completionRate"), summaryMap.get("completionRate")))
                        .build();
            }
        }

        return RevenueStatisticsResponse.builder()
                .summary(summary)
                .comparison(comparison)
                .revenueTrend(revenueTrend)
                .revenueByService(revenueByService)
                .revenueByPaymentMethod(revenueByPaymentMethod)
                .goals(goals)
                .build();
    }

    private RevenueStatisticsResponse.Summary buildRevenueSummary(Map<String, Object> map, LocalDate startDate, LocalDate endDate) {
        long totalRevenue = toLong(map.get("totalRevenue"));
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long averageRevenue = days > 0 ? totalRevenue / days : 0;
        return RevenueStatisticsResponse.Summary.builder()
                .totalRevenue(totalRevenue)
                .averageRevenue(averageRevenue)
                .averageTransactionAmount(toLong(map.get("averageTransactionAmount")))
                .completionRate(toDouble(map.get("completionRate")))
                .customerLTV(toLong(map.get("customerLTV")))
                .averageServiceDuration(toInt(map.get("averageServiceDuration")))
                .build();
    }

    private RevenueStatisticsResponse.GoalProgress buildGoalProgress(
            Long businessId, LocalDate startDate, LocalDate endDate, Map<String, Object> summaryMap) {

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        YearMonth targetMonth = YearMonth.from(endDate);
        int totalDays = targetMonth.lengthOfMonth();
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate today = LocalDate.now();
        LocalDate effectiveEnd = today.isBefore(targetMonth.atEndOfMonth()) ? today : targetMonth.atEndOfMonth();

        int daysElapsed = (int) ChronoUnit.DAYS.between(monthStart, effectiveEnd) + 1;
        int daysRemaining = totalDays - daysElapsed;
        if (daysRemaining < 0) daysRemaining = 0;
        if (daysElapsed > totalDays) daysElapsed = totalDays;

        Long currentRevenue = toLong(summaryMap.get("totalRevenue"));
        Integer completedCount = toInt(summaryMap.get("completedReservations"));

        // Revenue goal
        Integer monthlyRevenueGoalInt = business.getMonthlyRevenueGoal();
        Long revenueGoal = monthlyRevenueGoalInt != null ? monthlyRevenueGoalInt.longValue() : null;
        Double revenueAchievementRate = null;
        Long projectedRevenue = null;

        if (revenueGoal != null && revenueGoal > 0) {
            revenueAchievementRate = Math.round((currentRevenue * 1000.0 / revenueGoal)) / 10.0;
        }
        if (daysElapsed > 0) {
            projectedRevenue = currentRevenue * totalDays / daysElapsed;
        }

        // Reservation goal (derive from daily average * total days if no explicit goal)
        Integer reservationGoal = null;
        Double reservationAchievementRate = null;
        Integer projectedReservations = null;

        if (completedCount != null && daysElapsed > 0) {
            projectedReservations = completedCount * totalDays / daysElapsed;
        }

        return RevenueStatisticsResponse.GoalProgress.builder()
                .revenueGoal(revenueGoal)
                .revenueAchievementRate(revenueAchievementRate)
                .projectedRevenue(projectedRevenue)
                .reservationGoal(reservationGoal)
                .reservationAchievementRate(reservationAchievementRate)
                .projectedReservations(projectedReservations)
                .daysElapsed(daysElapsed)
                .daysRemaining(daysRemaining)
                .totalDays(totalDays)
                .build();
    }

    // ========================================
    // 2. Reservation Statistics (예약 통계)
    // ========================================

    public ReservationStatisticsResponse getReservationStatistics(Long businessId, StatisticsSearchCondition condition) {
        validateSearchCondition(condition);
        validateBusinessExists(businessId);

        LocalDate startDate = condition.getStartDate();
        LocalDate endDate = condition.getEndDate();
        String groupBy = condition.getGroupBy() != null ? condition.getGroupBy() : "daily";

        // 1. Summary
        Map<String, Object> summaryMap = statisticsRepository.getReservationSummary(businessId, startDate, endDate);
        ReservationStatisticsResponse.Summary summary = buildReservationSummary(summaryMap);

        // 2. Trend
        List<Map<String, Object>> trendRows = statisticsRepository.getReservationTrend(businessId, startDate, endDate, groupBy);
        List<ReservationStatisticsResponse.ReservationTrendItem> reservationTrend = trendRows.stream()
                .map(row -> ReservationStatisticsResponse.ReservationTrendItem.builder()
                        .date(toStr(row.get("date")))
                        .total(toInt(row.get("total")))
                        .completed(toInt(row.get("completed")))
                        .cancelled(toInt(row.get("cancelled")))
                        .noShow(toInt(row.get("noShow")))
                        .pending(toInt(row.get("pending")))
                        .build())
                .toList();

        // 3. Hourly heatmap (flat list -> nested structure)
        List<Map<String, Object>> heatmapRows = statisticsRepository.getHourlyHeatmap(businessId, startDate, endDate);
        List<ReservationStatisticsResponse.HourlyHeatmapItem> hourlyHeatmap = buildHourlyHeatmap(heatmapRows);

        // 4. Status distribution
        List<Map<String, Object>> statusRows = statisticsRepository.getStatusDistribution(businessId, startDate, endDate);
        List<ReservationStatisticsResponse.StatusDistributionItem> statusDistribution = statusRows.stream()
                .map(row -> ReservationStatisticsResponse.StatusDistributionItem.builder()
                        .status(toStr(row.get("status")))
                        .statusName(toStr(row.get("statusName")))
                        .count(toInt(row.get("count")))
                        .percentage(toDouble(row.get("percentage")))
                        .build())
                .toList();

        // 5. Daily distribution
        List<Map<String, Object>> dailyRows = statisticsRepository.getDailyDistribution(businessId, startDate, endDate);
        List<ReservationStatisticsResponse.DailyDistributionItem> dailyDistribution = dailyRows.stream()
                .map(row -> {
                    int dayOfWeek = toInt(row.get("dayOfWeek"));
                    return ReservationStatisticsResponse.DailyDistributionItem.builder()
                            .dayOfWeek(dayOfWeek)
                            .dayName(getDayName(dayOfWeek))
                            .averageCount(toDouble(row.get("averageCount")))
                            .totalCount(toInt(row.get("totalCount")))
                            .build();
                })
                .toList();

        // 6. Comparison (optional)
        ReservationStatisticsResponse.Comparison comparison = null;
        if (condition.getCompareWith() != null) {
            LocalDate[] compDates = getComparisonDates(startDate, endDate, condition.getCompareWith());
            if (compDates != null) {
                Map<String, Object> compSummary = statisticsRepository.getReservationSummary(
                        businessId, compDates[0], compDates[1]);
                comparison = ReservationStatisticsResponse.Comparison.builder()
                        .totalChange(calcPctChange(compSummary.get("totalReservations"), summaryMap.get("totalReservations")))
                        .completedChange(calcPctChange(compSummary.get("completedReservations"), summaryMap.get("completedReservations")))
                        .cancelledChange(calcPctChange(compSummary.get("cancelledReservations"), summaryMap.get("cancelledReservations")))
                        .noShowChange(calcPctChange(compSummary.get("noShowReservations"), summaryMap.get("noShowReservations")))
                        .build();
            }
        }

        return ReservationStatisticsResponse.builder()
                .summary(summary)
                .comparison(comparison)
                .reservationTrend(reservationTrend)
                .hourlyHeatmap(hourlyHeatmap)
                .statusDistribution(statusDistribution)
                .dailyDistribution(dailyDistribution)
                .build();
    }

    private ReservationStatisticsResponse.Summary buildReservationSummary(Map<String, Object> map) {
        return ReservationStatisticsResponse.Summary.builder()
                .totalReservations(toInt(map.get("totalReservations")))
                .completedReservations(toInt(map.get("completedReservations")))
                .cancelledReservations(toInt(map.get("cancelledReservations")))
                .noShowReservations(toInt(map.get("noShowReservations")))
                .pendingReservations(toInt(map.get("pendingReservations")))
                .completionRate(toDouble(map.get("completionRate")))
                .cancellationRate(toDouble(map.get("cancellationRate")))
                .noShowRate(toDouble(map.get("noShowRate")))
                .lostRevenue(toLong(map.get("lostRevenue")))
                .build();
    }

    private List<ReservationStatisticsResponse.HourlyHeatmapItem> buildHourlyHeatmap(List<Map<String, Object>> rows) {
        // Group flat rows by dayOfWeek
        Map<Integer, List<ReservationStatisticsResponse.HourCount>> grouped = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            int dayOfWeek = toInt(row.get("dayOfWeek"));
            int hour = toInt(row.get("hour"));
            int count = toInt(row.get("count"));

            grouped.computeIfAbsent(dayOfWeek, k -> new ArrayList<>())
                    .add(ReservationStatisticsResponse.HourCount.builder()
                            .hour(hour)
                            .count(count)
                            .build());
        }

        return grouped.entrySet().stream()
                .map(entry -> ReservationStatisticsResponse.HourlyHeatmapItem.builder()
                        .dayOfWeek(entry.getKey())
                        .dayName(getDayName(entry.getKey()))
                        .hours(entry.getValue())
                        .build())
                .toList();
    }

    // ========================================
    // 3. Customer Statistics (고객 통계)
    // ========================================

    public CustomerStatisticsResponse getCustomerStatistics(Long businessId, StatisticsSearchCondition condition) {
        validateSearchCondition(condition);
        validateBusinessExists(businessId);

        LocalDate startDate = condition.getStartDate();
        LocalDate endDate = condition.getEndDate();

        // 1. Summary
        Map<String, Object> summaryMap = statisticsRepository.getCustomerSummary(businessId, startDate, endDate);
        CustomerStatisticsResponse.Summary summary = buildCustomerSummary(summaryMap);

        // 2. Customer trend
        List<Map<String, Object>> trendRows = statisticsRepository.getCustomerTrend(businessId, startDate, endDate);
        List<CustomerStatisticsResponse.CustomerTrendItem> customerTrend = trendRows.stream()
                .map(row -> CustomerStatisticsResponse.CustomerTrendItem.builder()
                        .date(toStr(row.get("date")))
                        .newCustomers(toInt(row.get("newCustomers")))
                        .returningCustomers(toInt(row.get("returningCustomers")))
                        .totalActive(toInt(row.get("totalActive")))
                        .churned(toInt(row.get("churned")))
                        .build())
                .toList();

        // 3. Segments
        List<Map<String, Object>> segmentRows = statisticsRepository.getCustomerSegments(businessId);
        int totalCustomers = summary.getTotalCustomers() != null ? summary.getTotalCustomers() : 0;
        List<CustomerStatisticsResponse.SegmentItem> segments = segmentRows.stream()
                .map(row -> {
                    int count = toInt(row.get("count"));
                    double percentage = totalCustomers > 0
                            ? Math.round(count * 1000.0 / totalCustomers) / 10.0
                            : 0.0;
                    return CustomerStatisticsResponse.SegmentItem.builder()
                            .segment(toStr(row.get("segment")))
                            .segmentName(toStr(row.get("segmentName")))
                            .description(toStr(row.get("description")))
                            .count(count)
                            .percentage(percentage)
                            .totalRevenue(toLong(row.get("totalRevenue")))
                            .averageRevenue(toLong(row.get("averageRevenue")))
                            .build();
                })
                .toList();

        // 4. Returning rate trend
        List<Map<String, Object>> returningRows = statisticsRepository.getReturningRateTrend(businessId, startDate, endDate);
        List<CustomerStatisticsResponse.ReturningRateTrendItem> returningRateTrend = returningRows.stream()
                .map(row -> CustomerStatisticsResponse.ReturningRateTrendItem.builder()
                        .date(toStr(row.get("date")))
                        .rate(toDouble(row.get("rate")))
                        .build())
                .toList();

        // 5. LTV distribution
        List<Map<String, Object>> ltvRows = statisticsRepository.getLtvDistribution(businessId);
        List<CustomerStatisticsResponse.LtvDistributionItem> ltvDistribution = ltvRows.stream()
                .map(row -> CustomerStatisticsResponse.LtvDistributionItem.builder()
                        .range(toStr(row.get("range")))
                        .min(toLong(row.get("min")))
                        .max(toLong(row.get("max")))
                        .count(toInt(row.get("count")))
                        .build())
                .toList();

        // 6. Comparison (optional)
        CustomerStatisticsResponse.Comparison comparison = null;
        if (condition.getCompareWith() != null) {
            LocalDate[] compDates = getComparisonDates(startDate, endDate, condition.getCompareWith());
            if (compDates != null) {
                Map<String, Object> compSummary = statisticsRepository.getCustomerSummary(
                        businessId, compDates[0], compDates[1]);
                comparison = CustomerStatisticsResponse.Comparison.builder()
                        .totalCustomersChange(calcPctChange(compSummary.get("totalCustomers"), summaryMap.get("totalCustomers")))
                        .newCustomersChange(calcPctChange(compSummary.get("newCustomers"), summaryMap.get("newCustomers")))
                        .returningRateChange(calcPctChange(compSummary.get("returningRate"), summaryMap.get("returningRate")))
                        .averageVisitCountChange(calcPctChange(compSummary.get("averageVisitCount"), summaryMap.get("averageVisitCount")))
                        .build();
            }
        }

        return CustomerStatisticsResponse.builder()
                .summary(summary)
                .comparison(comparison)
                .customerTrend(customerTrend)
                .segments(segments)
                .returningRateTrend(returningRateTrend)
                .ltvDistribution(ltvDistribution)
                .build();
    }

    private CustomerStatisticsResponse.Summary buildCustomerSummary(Map<String, Object> map) {
        return CustomerStatisticsResponse.Summary.builder()
                .totalCustomers(toInt(map.get("totalCustomers")))
                .newCustomers(toInt(map.get("newCustomers")))
                .returningRate(toDouble(map.get("returningRate")))
                .averageVisitCount(toDouble(map.get("averageVisitCount")))
                .averageLTV(toLong(map.get("averageLTV")))
                .churnRate(toDouble(map.get("churnRate")))
                .build();
    }

    // ========================================
    // 4. Staff Statistics (스태프 통계)
    // ========================================

    public StaffStatisticsResponse getStaffStatistics(Long businessId, StatisticsSearchCondition condition) {
        validateSearchCondition(condition);
        validateBusinessExists(businessId);

        LocalDate startDate = condition.getStartDate();
        LocalDate endDate = condition.getEndDate();
        Long staffId = condition.getStaffId();

        // Validate staffId if provided
        if (staffId != null) {
            staffRepository.findById(staffId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            ErrorCode.STATISTICS_STAFF_NOT_FOUND,
                            "통계 조회 대상 직원을 찾을 수 없습니다: " + staffId));
        }

        // 1. Staff performances
        List<Map<String, Object>> perfRows = statisticsRepository.getStaffPerformances(businessId, startDate, endDate, staffId);
        List<StaffStatisticsResponse.StaffPerformanceItem> staffPerformances = perfRows.stream()
                .map(row -> StaffStatisticsResponse.StaffPerformanceItem.builder()
                        .staffId(toLong(row.get("staffId")))
                        .staffName(toStr(row.get("staffName")))
                        .positionName(toStr(row.get("positionName")))
                        .profileImageUrl(toStr(row.get("profileImageUrl")))
                        .reservationCount(toInt(row.get("reservationCount")))
                        .completedCount(toInt(row.get("completedCount")))
                        .cancelledCount(toInt(row.get("cancelledCount")))
                        .noShowCount(toInt(row.get("noShowCount")))
                        .totalRevenue(toLong(row.get("totalRevenue")))
                        .averageRevenue(toLong(row.get("averageRevenue")))
                        .averageDuration(toInt(row.get("averageDuration")))
                        .completionRate(toDouble(row.get("completionRate")))
                        .customerCount(toInt(row.get("customerCount")))
                        .build())
                .toList();

        // 2. Staff revenue trend (flat -> nested by staffId)
        List<Map<String, Object>> trendRows = statisticsRepository.getStaffRevenueTrend(businessId, startDate, endDate, staffId);
        List<StaffStatisticsResponse.StaffRevenueTrendItem> staffRevenueTrend = buildStaffRevenueTrend(trendRows);

        // 3. Staff radar data (normalize to 0-100)
        List<Map<String, Object>> radarRows = statisticsRepository.getStaffRadarData(businessId, startDate, endDate);
        List<StaffStatisticsResponse.StaffRadarItem> staffRadar = buildStaffRadar(radarRows);

        // 4. Comparison (optional)
        StaffStatisticsResponse.Comparison comparison = null;
        if (condition.getCompareWith() != null) {
            LocalDate[] compDates = getComparisonDates(startDate, endDate, condition.getCompareWith());
            if (compDates != null) {
                List<Map<String, Object>> compPerfRows = statisticsRepository.getStaffPerformances(
                        businessId, compDates[0], compDates[1], staffId);

                // Calculate totals for current and comparison periods
                int currentTotalReservations = staffPerformances.stream()
                        .mapToInt(StaffStatisticsResponse.StaffPerformanceItem::getReservationCount)
                        .sum();
                long currentTotalRevenue = staffPerformances.stream()
                        .mapToLong(StaffStatisticsResponse.StaffPerformanceItem::getTotalRevenue)
                        .sum();

                int compTotalReservations = compPerfRows.stream()
                        .mapToInt(row -> toInt(row.get("reservationCount")))
                        .sum();
                long compTotalRevenue = compPerfRows.stream()
                        .mapToLong(row -> toLong(row.get("totalRevenue")))
                        .sum();

                comparison = StaffStatisticsResponse.Comparison.builder()
                        .totalReservationsChange(calcPctChange(compTotalReservations, currentTotalReservations))
                        .totalRevenueChange(calcPctChange(compTotalRevenue, currentTotalRevenue))
                        .build();
            }
        }

        return StaffStatisticsResponse.builder()
                .staffPerformances(staffPerformances)
                .comparison(comparison)
                .staffRevenueTrend(staffRevenueTrend)
                .staffRadar(staffRadar)
                .build();
    }

    private List<StaffStatisticsResponse.StaffRevenueTrendItem> buildStaffRevenueTrend(List<Map<String, Object>> rows) {
        // Group by staffId preserving insertion order
        Map<Long, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        Map<Long, String> staffNames = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            Long sid = toLong(row.get("staffId"));
            grouped.computeIfAbsent(sid, k -> new ArrayList<>()).add(row);
            staffNames.putIfAbsent(sid, toStr(row.get("staffName")));
        }

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<StaffStatisticsResponse.TrendItem> trend = entry.getValue().stream()
                            .map(row -> StaffStatisticsResponse.TrendItem.builder()
                                    .date(toStr(row.get("date")))
                                    .revenue(toLong(row.get("revenue")))
                                    .reservationCount(toInt(row.get("reservationCount")))
                                    .build())
                            .toList();
                    return StaffStatisticsResponse.StaffRevenueTrendItem.builder()
                            .staffId(entry.getKey())
                            .staffName(staffNames.get(entry.getKey()))
                            .trend(trend)
                            .build();
                })
                .toList();
    }

    private List<StaffStatisticsResponse.StaffRadarItem> buildStaffRadar(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }

        // Find max values for normalization
        int maxReservationVolume = rows.stream()
                .mapToInt(row -> toInt(row.get("reservationVolume")))
                .max().orElse(1);
        long maxRevenue = rows.stream()
                .mapToLong(row -> toLong(row.get("revenue")))
                .max().orElse(1L);
        int maxDuration = rows.stream()
                .mapToInt(row -> toInt(row.get("averageDuration")))
                .max().orElse(1);
        int minDuration = rows.stream()
                .mapToInt(row -> toInt(row.get("averageDuration")))
                .filter(d -> d > 0)
                .min().orElse(1);

        final int finalMaxReservationVolume = Math.max(maxReservationVolume, 1);
        final long finalMaxRevenue = Math.max(maxRevenue, 1L);
        final int finalMaxDuration = Math.max(maxDuration, 1);
        final int finalMinDuration = Math.max(minDuration, 1);

        return rows.stream()
                .map(row -> {
                    int reservationVolume = toInt(row.get("reservationVolume"));
                    long revenue = toLong(row.get("revenue"));
                    double completionRate = toDouble(row.get("completionRate"));
                    double avgRating = toDouble(row.get("averageRating"));
                    int avgDuration = toInt(row.get("averageDuration"));

                    // Normalize to 0-100 scale
                    int normReservationVolume = (int) Math.round((double) reservationVolume / finalMaxReservationVolume * 100);
                    int normRevenue = (int) Math.round((double) revenue / finalMaxRevenue * 100);
                    int normCompletionRate = (int) Math.round(completionRate);
                    int normCustomerSatisfaction = (int) Math.round((avgRating / 5.0) * 100);

                    // Efficiency: lower duration = higher efficiency
                    int normEfficiency;
                    if (avgDuration <= 0) {
                        normEfficiency = 0;
                    } else {
                        normEfficiency = (int) Math.round(Math.min((double) finalMinDuration / avgDuration * 100, 100));
                    }

                    StaffStatisticsResponse.RadarMetrics metrics = StaffStatisticsResponse.RadarMetrics.builder()
                            .reservationVolume(normReservationVolume)
                            .revenue(normRevenue)
                            .completionRate(normCompletionRate)
                            .customerSatisfaction(normCustomerSatisfaction)
                            .efficiency(normEfficiency)
                            .build();

                    return StaffStatisticsResponse.StaffRadarItem.builder()
                            .staffId(toLong(row.get("staffId")))
                            .staffName(toStr(row.get("staffName")))
                            .metrics(metrics)
                            .build();
                })
                .toList();
    }

    // ========================================
    // 5. Service Statistics (서비스 통계)
    // ========================================

    public ServiceStatisticsResponse getServiceStatistics(Long businessId, StatisticsSearchCondition condition) {
        validateSearchCondition(condition);
        validateBusinessExists(businessId);

        LocalDate startDate = condition.getStartDate();
        LocalDate endDate = condition.getEndDate();
        Long categoryId = condition.getCategoryId();

        // 1. Summary
        Map<String, Object> summaryMap = statisticsRepository.getServiceSummary(businessId, startDate, endDate);
        ServiceStatisticsResponse.Summary summary = buildServiceSummary(summaryMap);

        // 2. Rankings
        List<Map<String, Object>> rankingRows = statisticsRepository.getServiceRankings(businessId, startDate, endDate, categoryId);
        List<ServiceStatisticsResponse.ServiceRankingItem> serviceRankings = new ArrayList<>();
        for (int i = 0; i < rankingRows.size(); i++) {
            Map<String, Object> row = rankingRows.get(i);
            serviceRankings.add(ServiceStatisticsResponse.ServiceRankingItem.builder()
                    .rank(i + 1)
                    .serviceId(toLong(row.get("serviceId")))
                    .serviceName(toStr(row.get("serviceName")))
                    .categoryId(toLong(row.get("categoryId")))
                    .categoryName(toStr(row.get("categoryName")))
                    .reservationCount(toInt(row.get("reservationCount")))
                    .totalRevenue(toLong(row.get("totalRevenue")))
                    .averagePrice(toLong(row.get("averagePrice")))
                    .revenuePercentage(toDouble(row.get("revenuePercentage")))
                    .averageDuration(toInt(row.get("averageDuration")))
                    .completionRate(toDouble(row.get("completionRate")))
                    .build());
        }

        // 3. Category distribution
        List<Map<String, Object>> categoryRows = statisticsRepository.getCategoryDistribution(businessId, startDate, endDate);
        List<ServiceStatisticsResponse.CategoryDistributionItem> categoryDistribution = categoryRows.stream()
                .map(row -> ServiceStatisticsResponse.CategoryDistributionItem.builder()
                        .categoryId(toLong(row.get("categoryId")))
                        .categoryName(toStr(row.get("categoryName")))
                        .serviceCount(toInt(row.get("serviceCount")))
                        .reservationCount(toInt(row.get("reservationCount")))
                        .revenue(toLong(row.get("revenue")))
                        .percentage(toDouble(row.get("percentage")))
                        .build())
                .toList();

        // 4. Service trend (flat -> nested by serviceId)
        List<Map<String, Object>> trendRows = statisticsRepository.getServiceTrend(businessId, startDate, endDate);
        List<ServiceStatisticsResponse.ServiceTrendItem> serviceTrend = buildServiceTrend(trendRows);

        // 5. Comparison (optional)
        ServiceStatisticsResponse.Comparison comparison = null;
        if (condition.getCompareWith() != null) {
            LocalDate[] compDates = getComparisonDates(startDate, endDate, condition.getCompareWith());
            if (compDates != null) {
                Map<String, Object> compSummary = statisticsRepository.getServiceSummary(
                        businessId, compDates[0], compDates[1]);
                comparison = ServiceStatisticsResponse.Comparison.builder()
                        .totalServiceCountChange(calcPctChange(compSummary.get("totalServiceCount"), summaryMap.get("totalServiceCount")))
                        .averagePriceChange(calcPctChange(compSummary.get("averagePrice"), summaryMap.get("averagePrice")))
                        .build();
            }
        }

        return ServiceStatisticsResponse.builder()
                .summary(summary)
                .comparison(comparison)
                .serviceRankings(serviceRankings)
                .categoryDistribution(categoryDistribution)
                .serviceTrend(serviceTrend)
                .build();
    }

    private ServiceStatisticsResponse.Summary buildServiceSummary(Map<String, Object> map) {
        return ServiceStatisticsResponse.Summary.builder()
                .totalServiceCount(toInt(map.get("totalServiceCount")))
                .uniqueServiceTypes(toInt(map.get("uniqueServiceTypes")))
                .averagePrice(toLong(map.get("averagePrice")))
                .categoryCount(toInt(map.get("categoryCount")))
                .mostPopularService(toStr(map.get("mostPopularService")))
                .mostProfitableService(toStr(map.get("mostProfitableService")))
                .build();
    }

    private List<ServiceStatisticsResponse.ServiceTrendItem> buildServiceTrend(List<Map<String, Object>> rows) {
        // Group by serviceId preserving insertion order
        Map<Long, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        Map<Long, String> serviceNames = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            Long sid = toLong(row.get("serviceId"));
            grouped.computeIfAbsent(sid, k -> new ArrayList<>()).add(row);
            serviceNames.putIfAbsent(sid, toStr(row.get("serviceName")));
        }

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<ServiceStatisticsResponse.TrendItem> trend = entry.getValue().stream()
                            .map(row -> ServiceStatisticsResponse.TrendItem.builder()
                                    .date(toStr(row.get("date")))
                                    .revenue(toLong(row.get("revenue")))
                                    .count(toInt(row.get("count")))
                                    .build())
                            .toList();
                    return ServiceStatisticsResponse.ServiceTrendItem.builder()
                            .serviceId(entry.getKey())
                            .serviceName(serviceNames.get(entry.getKey()))
                            .trend(trend)
                            .build();
                })
                .toList();
    }

    // ========================================
    // Validation helpers
    // ========================================

    private void validateSearchCondition(StatisticsSearchCondition condition) {
        if (condition.getStartDate().isAfter(condition.getEndDate())) {
            throw new BusinessException(ErrorCode.STATISTICS_INVALID_DATE_RANGE,
                    "시작일은 종료일보다 이전이어야 합니다");
        }

        long daysBetween = ChronoUnit.DAYS.between(condition.getStartDate(), condition.getEndDate());
        if (daysBetween > 365) {
            throw new BusinessException(ErrorCode.STATISTICS_DATE_RANGE_EXCEEDED,
                    "조회 가능 기간은 최대 1년입니다");
        }

        String groupBy = condition.getGroupBy();
        if (groupBy != null && !List.of("daily", "weekly", "monthly").contains(groupBy)) {
            throw new BusinessException(ErrorCode.STATISTICS_INVALID_GROUP_BY,
                    "유효하지 않은 집계 단위입니다: " + groupBy);
        }
    }

    private void validateBusinessExists(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }
    }

    // ========================================
    // Comparison period calculation
    // ========================================

    private LocalDate[] getComparisonDates(LocalDate startDate, LocalDate endDate, String compareWith) {
        if (compareWith == null) {
            return null;
        }

        return switch (compareWith) {
            case "PREVIOUS_PERIOD" -> {
                LocalDate compEnd = startDate.minusDays(1);
                long days = ChronoUnit.DAYS.between(startDate, endDate);
                LocalDate compStart = compEnd.minusDays(days);
                yield new LocalDate[]{compStart, compEnd};
            }
            case "LAST_YEAR" -> {
                LocalDate compStart = startDate.minusYears(1);
                LocalDate compEnd = endDate.minusYears(1);
                yield new LocalDate[]{compStart, compEnd};
            }
            default -> null;
        };
    }

    // ========================================
    // Percentage change calculation
    // ========================================

    private Double calcPctChange(Number prev, Number cur) {
        if (prev == null || prev.doubleValue() == 0) {
            return cur != null && cur.doubleValue() > 0 ? 100.0 : 0.0;
        }
        double rate = ((cur.doubleValue() - prev.doubleValue()) / prev.doubleValue()) * 100;
        return Math.round(rate * 10.0) / 10.0;
    }

    private Double calcPctChange(Object prev, Object cur) {
        Number prevNum = prev instanceof Number n ? n : null;
        Number curNum = cur instanceof Number n ? n : null;
        return calcPctChange(prevNum, curNum);
    }

    // ========================================
    // Map value extraction helpers (null-safe)
    // ========================================

    private Long toLong(Object val) {
        return val instanceof Number n ? n.longValue() : 0L;
    }

    private Integer toInt(Object val) {
        return val instanceof Number n ? n.intValue() : 0;
    }

    private Double toDouble(Object val) {
        return val instanceof Number n ? Math.round(n.doubleValue() * 10.0) / 10.0 : 0.0;
    }

    private String toStr(Object val) {
        return val != null ? val.toString() : null;
    }

    // ========================================
    // Day name helper (ISO day of week)
    // ========================================

    private String getDayName(int isoDow) {
        return switch (isoDow) {
            case 1 -> "월";
            case 2 -> "화";
            case 3 -> "수";
            case 4 -> "목";
            case 5 -> "금";
            case 6 -> "토";
            case 7 -> "일";
            default -> "";
        };
    }
}
