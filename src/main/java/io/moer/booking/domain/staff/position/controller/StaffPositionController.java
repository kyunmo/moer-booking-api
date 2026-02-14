package io.moer.booking.domain.staff.position.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.domain.service.category.dto.SortOrderUpdateRequest;
import io.moer.booking.domain.staff.position.dto.StaffPositionCreateRequest;
import io.moer.booking.domain.staff.position.dto.StaffPositionResponse;
import io.moer.booking.domain.staff.position.dto.StaffPositionUpdateRequest;
import io.moer.booking.domain.staff.position.service.StaffPositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/businesses/{businessId}/staff-positions")
@RequiredArgsConstructor
@Tag(name = "Staff Position", description = "직급 관리 API")
public class StaffPositionController {

    private final StaffPositionService staffPositionService;

    /**
     * 직급 생성
     */
    @PostMapping
    @Operation(summary = "직급 생성")
    public ResponseEntity<ApiResponse<StaffPositionResponse>> createPosition(
            @PathVariable Long businessId,
            @Valid @RequestBody StaffPositionCreateRequest request) {
        StaffPositionResponse response = staffPositionService.createPosition(businessId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 직급 목록 조회 (sort_order 순)
     */
    @GetMapping
    @Operation(summary = "매장 직급 목록 조회")
    public ApiResponse<List<StaffPositionResponse>> getPositionsByBusiness(
            @PathVariable Long businessId) {
        List<StaffPositionResponse> response = staffPositionService.getPositionsByBusiness(businessId);
        return ApiResponse.success(response);
    }

    /**
     * 직급 단건 조회
     */
    @GetMapping("/{positionId}")
    @Operation(summary = "직급 단건 조회")
    public ApiResponse<StaffPositionResponse> getPosition(
            @PathVariable Long businessId,
            @PathVariable Long positionId) {
        StaffPositionResponse response = staffPositionService.getPosition(businessId, positionId);
        return ApiResponse.success(response);
    }

    /**
     * 직급 수정
     */
    @PatchMapping("/{positionId}")
    @Operation(summary = "직급 수정")
    public ApiResponse<StaffPositionResponse> updatePosition(
            @PathVariable Long businessId,
            @PathVariable Long positionId,
            @Valid @RequestBody StaffPositionUpdateRequest request) {
        StaffPositionResponse response = staffPositionService.updatePosition(businessId, positionId, request);
        return ApiResponse.success(response);
    }

    /**
     * 직급 삭제
     */
    @DeleteMapping("/{positionId}")
    @Operation(summary = "직급 삭제")
    public ApiResponse<Void> deletePosition(
            @PathVariable Long businessId,
            @PathVariable Long positionId) {
        staffPositionService.deletePosition(businessId, positionId);
        return ApiResponse.success();
    }

    /**
     * 직급 정렬 순서 변경
     */
    @PatchMapping("/sort-order")
    @Operation(summary = "직급 정렬 순서 변경")
    public ApiResponse<List<StaffPositionResponse>> updateSortOrder(
            @PathVariable Long businessId,
            @Valid @RequestBody SortOrderUpdateRequest request) {
        List<StaffPositionResponse> response = staffPositionService.updateSortOrder(businessId, request);
        return ApiResponse.success(response);
    }
}
