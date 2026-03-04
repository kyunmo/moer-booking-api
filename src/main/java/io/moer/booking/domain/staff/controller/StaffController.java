package io.moer.booking.domain.staff.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.domain.staff.dto.PortfolioResponse;
import io.moer.booking.domain.staff.dto.StaffCreateRequest;
import io.moer.booking.domain.staff.dto.StaffResponse;
import io.moer.booking.domain.staff.dto.StaffScheduleViewResponse;
import io.moer.booking.domain.staff.dto.StaffSearchCondition;
import io.moer.booking.domain.staff.dto.StaffUpdateRequest;
import io.moer.booking.domain.staff.service.PortfolioService;
import io.moer.booking.domain.staff.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/businesses/{businessId}/staffs")
@RequiredArgsConstructor
@Tag(name = "Staff", description = "직원 관리 API")
public class StaffController {

    private final StaffService staffService;
    private final PortfolioService portfolioService;

    /**
     * Staff 생성
     */
    @PostMapping
    @Operation(summary = "직원 생성")
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
    @Operation(summary = "직원 단건 조회")
    public ApiResponse<StaffResponse> getStaff(
            @PathVariable Long businessId,
            @PathVariable Long staffId) {
        StaffResponse response = staffService.getStaff(businessId, staffId);
        return ApiResponse.success(response);
    }

    /**
     * Staff 주간 스케줄 조회
     */
    @GetMapping("/{staffId}/schedule")
    @Operation(summary = "직원 주간 스케줄 조회", description = "직원의 근무 스케줄 + 예약 목록을 날짜 범위로 조회합니다")
    public ApiResponse<StaffScheduleViewResponse> getStaffSchedule(
            @PathVariable Long businessId,
            @PathVariable Long staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "week") String view) {
        StaffScheduleViewResponse response = staffService.getStaffSchedule(businessId, staffId, startDate, endDate);
        return ApiResponse.success(response);
    }

    /**
     * Business의 Staff 목록 조회 / 검색
     * 필터 파라미터가 없으면 기존 동작과 동일 (하위 호환)
     */
    @GetMapping
    @Operation(summary = "매장 직원 목록 조회/검색")
    public ApiResponse<List<StaffResponse>> getStaffsByBusiness(
            @PathVariable Long businessId,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long positionId,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) Integer minCareerYears,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {

        // 검색 파라미터가 하나라도 있으면 검색 모드
        boolean hasSearchParams = name != null || positionId != null || specialty != null
                || minCareerYears != null || sortBy != null || sortOrder != null;

        if (hasSearchParams) {
            StaffSearchCondition condition = StaffSearchCondition.builder()
                    .name(name)
                    .positionId(positionId)
                    .specialty(specialty)
                    .isActive(activeOnly ? true : null)
                    .minCareerYears(minCareerYears)
                    .sortBy(sortBy)
                    .sortOrder(sortOrder)
                    .build();
            List<StaffResponse> response = staffService.searchStaffs(businessId, condition);
            return ApiResponse.success(response);
        }

        // 기존 동작 유지
        List<StaffResponse> response = activeOnly
                ? staffService.getActiveStaffsByBusiness(businessId)
                : staffService.getStaffsByBusiness(businessId);
        return ApiResponse.success(response);
    }

    /**
     * Staff 수정
     */
    @PatchMapping("/{staffId}")
    @Operation(summary = "직원 정보 수정")
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
    @Operation(summary = "직원 활성/비활성 전환")
    public ApiResponse<StaffResponse> toggleStaffActive(
            @PathVariable Long businessId,
            @PathVariable Long staffId) {
        StaffResponse response = staffService.toggleStaffActive(businessId, staffId);
        return ApiResponse.success(response);
    }

    /**
     * Staff 프로필 이미지 업로드
     */
    @PostMapping(value = "/{staffId}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "직원 프로필 이미지 업로드")
    public ApiResponse<StaffResponse> uploadProfileImage(
            @PathVariable Long businessId,
            @PathVariable Long staffId,
            @RequestParam("file") MultipartFile file) {
        StaffResponse response = staffService.uploadProfileImage(businessId, staffId, file);
        return ApiResponse.success(response);
    }

    /**
     * Staff 포트폴리오 목록 조회
     */
    @GetMapping("/{staffId}/portfolios")
    @Operation(summary = "직원 포트폴리오 목록 조회")
    public ApiResponse<List<PortfolioResponse>> getPortfoliosByStaff(
            @PathVariable Long businessId,
            @PathVariable Long staffId) {
        List<PortfolioResponse> response = portfolioService.getPortfoliosByStaffWithAuth(businessId, staffId);
        return ApiResponse.success(response);
    }

    /**
     * Staff 포트폴리오 생성 (이미지 업로드)
     */
    @PostMapping(value = "/{staffId}/portfolios", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "직원 포트폴리오 생성 (이미지 업로드)")
    public ApiResponse<PortfolioResponse> createPortfolioWithImage(
            @PathVariable Long businessId,
            @PathVariable Long staffId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "serviceCategory", required = false) String serviceCategory) {
        PortfolioResponse response = portfolioService.createPortfolioWithImage(
                businessId, staffId, file, title, description, serviceCategory);
        return ApiResponse.success(response);
    }

    /**
     * Staff 포트폴리오 삭제
     */
    @DeleteMapping("/{staffId}/portfolios/{portfolioId}")
    @Operation(summary = "직원 포트폴리오 삭제")
    public ApiResponse<Void> deletePortfolio(
            @PathVariable Long businessId,
            @PathVariable Long staffId,
            @PathVariable Long portfolioId) {
        portfolioService.deletePortfolioWithAuth(businessId, staffId, portfolioId);
        return ApiResponse.success(null, "포트폴리오 이미지가 삭제되었습니다.");
    }

    /**
     * Staff 삭제
     */
    @DeleteMapping("/{staffId}")
    @Operation(summary = "직원 삭제")
    public ApiResponse<Void> deleteStaff(
            @PathVariable Long businessId,
            @PathVariable Long staffId) {
        staffService.deleteStaff(businessId, staffId);
        return ApiResponse.success();
    }
}