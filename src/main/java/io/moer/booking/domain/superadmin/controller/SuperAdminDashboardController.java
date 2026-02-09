package io.moer.booking.domain.superadmin.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.superadmin.dto.BusinessRevenueRank;
import io.moer.booking.domain.superadmin.dto.BusinessTypeStats;
import io.moer.booking.domain.superadmin.dto.SystemStats;
import io.moer.booking.domain.superadmin.service.SuperAdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 슈퍼 관리자 - 시스템 대시보드 API
 */
@RestController
@RequestMapping("/api/superadmin/dashboard")
@RequiredArgsConstructor
public class SuperAdminDashboardController {

    private final SuperAdminDashboardService dashboardService;

    /**
     * 시스템 전체 통계
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SystemStats>> getSystemStats(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (!currentUser.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
        }

        SystemStats stats = dashboardService.getSystemStats();

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 매장별 매출 랭킹
     */
    @GetMapping("/business-ranking")
    public ResponseEntity<ApiResponse<List<BusinessRevenueRank>>> getBusinessRanking(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {

        if (!currentUser.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
        }

        List<BusinessRevenueRank> ranking =
                dashboardService.getTopBusinesses(startDate, endDate, limit);

        return ResponseEntity.ok(ApiResponse.success(ranking));
    }

    /**
     * 업종별 통계
     */
    @GetMapping("/stats-by-type")
    public ResponseEntity<ApiResponse<List<BusinessTypeStats>>> getStatsByType(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (!currentUser.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
        }

        List<BusinessTypeStats> stats = dashboardService.getStatsByBusinessType();

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
