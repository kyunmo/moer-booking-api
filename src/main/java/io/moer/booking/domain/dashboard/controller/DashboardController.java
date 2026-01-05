package io.moer.booking.domain.dashboard.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.domain.dashboard.dto.DashboardResponse;
import io.moer.booking.domain.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/businesses/{businessId}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 대시보드 통계 조회
     */
    @GetMapping
    public ApiResponse<DashboardResponse> getDashboard(
            @PathVariable Long businessId,
            @RequestParam(required = false) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now();
        DashboardResponse response = dashboardService.getDashboardStats(businessId, targetDate);
        return ApiResponse.success(response);
    }
}