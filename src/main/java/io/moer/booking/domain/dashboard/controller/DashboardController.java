package io.moer.booking.domain.dashboard.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.dashboard.dto.BasicStatsResponse;
import io.moer.booking.domain.dashboard.dto.DashboardResponse;
import io.moer.booking.domain.dashboard.dto.GoalStatsResponse;
import io.moer.booking.domain.dashboard.dto.PeriodStatsResponse;
import io.moer.booking.domain.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;

@Tag(name = "대시보드", description = "대시보드 통계 API")
@RestController
@RequestMapping("/api/businesses/{businessId}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "대시보드 통계 조회", description = "전체 대시보드 통계를 조회합니다.")
    @GetMapping
    public ApiResponse<DashboardResponse> getDashboard(
            @PathVariable Long businessId,
            @RequestParam(required = false) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now();
        DashboardResponse response = dashboardService.getDashboardStats(businessId, targetDate);
        return ApiResponse.success(response);
    }

    @Operation(summary = "기본 통계 (FREE 플랜)", description = "모든 플랜에서 사용 가능한 기본 통계를 조회합니다.")
    @GetMapping("/basic-stats")
    public ApiResponse<BasicStatsResponse> getBasicStats(
            @PathVariable Long businessId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);
        LocalDate today = LocalDate.now();
        BasicStatsResponse response = dashboardService.getBasicStats(businessId, today);
        return ApiResponse.success(response);
    }

    @Operation(summary = "기간별 통계", description = "기간별 예약/매출 통계 및 이전 기간과 비교합니다.")
    @GetMapping("/stats")
    public ApiResponse<PeriodStatsResponse> getPeriodStats(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String compareWith) {
        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);
        return ApiResponse.success(dashboardService.getPeriodStats(businessId, startDate, endDate, compareWith));
    }

    @Operation(summary = "목표 달성률", description = "월별 매출/예약 목표 달성률을 조회합니다.")
    @GetMapping("/goals")
    public ApiResponse<GoalStatsResponse> getGoalStats(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @RequestParam(required = false) String month) {
        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);
        YearMonth targetMonth = month != null ? YearMonth.parse(month) : YearMonth.now();
        return ApiResponse.success(dashboardService.getGoalStats(businessId, targetMonth));
    }
}