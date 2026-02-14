package io.moer.booking.domain.staff.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.staff.dto.StaffAvailableTimesResponse;
import io.moer.booking.domain.staff.dto.StaffScheduleResponse;
import io.moer.booking.domain.staff.dto.StaffScheduleSaveRequest;
import io.moer.booking.domain.staff.service.StaffScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 스태프 근무 스케줄 Controller
 */
@Tag(name = "스태프 근무 스케줄", description = "스태프 요일별 근무시간 관리 API")
@RestController
@RequestMapping("/api/businesses/{businessId}/staffs/{staffId}")
@RequiredArgsConstructor
public class StaffScheduleController {

    private final StaffScheduleService staffScheduleService;

    /**
     * 직원 주간 근무 스케줄 조회
     */
    @GetMapping("/schedules")
    @Operation(summary = "직원 근무 스케줄 조회", description = "직원의 요일별 근무 스케줄(7일분)을 조회합니다")
    public ApiResponse<List<StaffScheduleResponse>> getSchedules(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @PathVariable Long staffId) {

        userDetails.canAccessBusiness(businessId);

        List<StaffScheduleResponse> response = staffScheduleService.getSchedules(businessId, staffId);
        return ApiResponse.success(response);
    }

    /**
     * 직원 주간 근무 스케줄 일괄 저장
     */
    @PutMapping("/schedules")
    @Operation(summary = "직원 근무 스케줄 일괄 저장", description = "직원의 요일별 근무 스케줄(7일분)을 일괄 저장합니다 (기존 스케줄 교체)")
    public ApiResponse<List<StaffScheduleResponse>> saveSchedules(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @PathVariable Long staffId,
            @Valid @RequestBody StaffScheduleSaveRequest request) {

        userDetails.canAccessBusiness(businessId);

        List<StaffScheduleResponse> response = staffScheduleService.saveSchedules(businessId, staffId, request);
        return ApiResponse.success(response);
    }

    /**
     * 특정 날짜의 직원 가용 시간 조회
     */
    @GetMapping("/available-times")
    @Operation(summary = "직원 가용 시간 조회", description = "특정 날짜에 해당 직원의 예약 가능한 시간 슬롯을 조회합니다")
    public ApiResponse<StaffAvailableTimesResponse> getAvailableTimes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @PathVariable Long staffId,
            @Parameter(description = "조회 날짜 (yyyy-MM-dd)", example = "2026-02-14")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        userDetails.canAccessBusiness(businessId);

        StaffAvailableTimesResponse response = staffScheduleService.getAvailableTimes(businessId, staffId, date);
        return ApiResponse.success(response);
    }
}
