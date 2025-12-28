package io.moer.booking.domain.staff.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.domain.staff.dto.StaffCreateRequest;
import io.moer.booking.domain.staff.dto.StaffResponse;
import io.moer.booking.domain.staff.dto.StaffUpdateRequest;
import io.moer.booking.domain.staff.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/businesses/{businessId}/staffs")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    /**
     * Staff 생성
     */
    @PostMapping
    public ApiResponse<StaffResponse> createStaff(
            @PathVariable Long businessId,
            @Valid @RequestBody StaffCreateRequest request) {
        StaffResponse response = staffService.createStaff(businessId, request);
        return ApiResponse.success(response);
    }

    /**
     * Staff 단건 조회
     */
    @GetMapping("/{staffId}")
    public ApiResponse<StaffResponse> getStaff(
            @PathVariable Long businessId,
            @PathVariable Long staffId) {
        StaffResponse response = staffService.getStaff(businessId, staffId);
        return ApiResponse.success(response);
    }

    /**
     * Business의 Staff 목록 조회
     */
    @GetMapping
    public ApiResponse<List<StaffResponse>> getStaffsByBusiness(
            @PathVariable Long businessId,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        List<StaffResponse> response = activeOnly
                ? staffService.getActiveStaffsByBusiness(businessId)
                : staffService.getStaffsByBusiness(businessId);
        return ApiResponse.success(response);
    }

    /**
     * Staff 수정
     */
    @PatchMapping("/{staffId}")
    public ApiResponse<StaffResponse> updateStaff(
            @PathVariable Long businessId,
            @PathVariable Long staffId,
            @Valid @RequestBody StaffUpdateRequest request) {
        StaffResponse response = staffService.updateStaff(businessId, staffId, request);
        return ApiResponse.success(response);
    }

    /**
     * Staff 활성/비활성 전환
     */
    @PatchMapping("/{staffId}/toggle-active")
    public ApiResponse<StaffResponse> toggleStaffActive(
            @PathVariable Long businessId,
            @PathVariable Long staffId) {
        StaffResponse response = staffService.toggleStaffActive(businessId, staffId);
        return ApiResponse.success(response);
    }

    /**
     * Staff 삭제
     */
    @DeleteMapping("/{staffId}")
    public ApiResponse<Void> deleteStaff(
            @PathVariable Long businessId,
            @PathVariable Long staffId) {
        staffService.deleteStaff(businessId, staffId);
        return ApiResponse.success();
    }
}