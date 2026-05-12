package io.moer.booking.domain.statistics.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.statistics.dto.*;
import io.moer.booking.domain.statistics.service.StatisticsService;
import io.moer.booking.domain.subscription.service.SubscriptionCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "통계 분석", description = "매출/예약/고객/직원/서비스 통계 분석 API (유료 전용)")
@RestController
@RequestMapping("/api/businesses/{businessId}/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final SubscriptionCheckService subscriptionCheckService;

    @Operation(summary = "매출 분석", description = "지정 기간의 매출 상세 분석 데이터를 조회합니다. (유료 전용)")
    @GetMapping("/revenue")
    public ApiResponse<RevenueStatisticsResponse> getRevenueStatistics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @Valid StatisticsSearchCondition condition) {
        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);
        subscriptionCheckService.checkPremiumAccess(businessId);
        return ApiResponse.success(statisticsService.getRevenueStatistics(businessId, condition));
    }

    @Operation(summary = "예약 분석", description = "지정 기간의 예약 상세 분석 데이터를 조회합니다. (유료 전용)")
    @GetMapping("/reservations")
    public ApiResponse<ReservationStatisticsResponse> getReservationStatistics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @Valid StatisticsSearchCondition condition) {
        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);
        subscriptionCheckService.checkPremiumAccess(businessId);
        return ApiResponse.success(statisticsService.getReservationStatistics(businessId, condition));
    }

    @Operation(summary = "고객 분석", description = "지정 기간의 고객 상세 분석 데이터를 조회합니다. (유료 전용)")
    @GetMapping("/customers")
    public ApiResponse<CustomerStatisticsResponse> getCustomerStatistics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @Valid StatisticsSearchCondition condition) {
        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);
        subscriptionCheckService.checkPremiumAccess(businessId);
        return ApiResponse.success(statisticsService.getCustomerStatistics(businessId, condition));
    }

    @Operation(summary = "직원 성과", description = "지정 기간의 직원별 성과 분석 데이터를 조회합니다. (유료 전용)")
    @GetMapping("/staff")
    public ApiResponse<StaffStatisticsResponse> getStaffStatistics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @Valid StatisticsSearchCondition condition) {
        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);
        subscriptionCheckService.checkPremiumAccess(businessId);
        return ApiResponse.success(statisticsService.getStaffStatistics(businessId, condition));
    }

    @Operation(summary = "서비스 분석", description = "지정 기간의 서비스별 분석 데이터를 조회합니다. (유료 전용)")
    @GetMapping("/services")
    public ApiResponse<ServiceStatisticsResponse> getServiceStatistics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @Valid StatisticsSearchCondition condition) {
        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);
        subscriptionCheckService.checkPremiumAccess(businessId);
        return ApiResponse.success(statisticsService.getServiceStatistics(businessId, condition));
    }
}
