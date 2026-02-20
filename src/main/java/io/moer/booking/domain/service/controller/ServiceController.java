package io.moer.booking.domain.service.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.domain.service.category.dto.SortOrderUpdateRequest;
import io.moer.booking.domain.service.dto.ServiceCreateRequest;
import io.moer.booking.domain.service.dto.ServiceResponse;
import io.moer.booking.domain.service.dto.ServiceSearchCondition;
import io.moer.booking.domain.service.dto.ServiceUpdateRequest;
import io.moer.booking.domain.service.service.ServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/businesses/{businessId}/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    /**
     * Service 생성
     */
    @PostMapping
    public ApiResponse<ServiceResponse> createService(
            @PathVariable Long businessId,
            @Valid @RequestBody ServiceCreateRequest request) {
        ServiceResponse response = serviceService.createService(businessId, request);
        return ApiResponse.success(response);
    }

    /**
     * Service 단건 조회
     */
    @GetMapping("/{serviceId}")
    public ApiResponse<ServiceResponse> getService(
            @PathVariable Long businessId,
            @PathVariable Long serviceId) {
        ServiceResponse response = serviceService.getService(businessId, serviceId);
        return ApiResponse.success(response);
    }

    /**
     * Business의 Service 목록 조회
     */
    @GetMapping
    public ApiResponse<List<ServiceResponse>> getServicesByBusiness(
            @PathVariable Long businessId,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly,
            @RequestParam(required = false) Long categoryId) {

        List<ServiceResponse> response;

        if (categoryId != null) {
            // 카테고리별 조회
            response = serviceService.getServicesByCategory(businessId, categoryId);
        } else if (activeOnly) {
            // 활성 서비스만 조회
            response = serviceService.getActiveServicesByBusiness(businessId);
        } else {
            // 전체 조회
            response = serviceService.getServicesByBusiness(businessId);
        }

        return ApiResponse.success(response);
    }

    /**
     * 서비스 이름 중복 확인
     * GET /api/businesses/{businessId}/services/check-name?name=커트&excludeId=5
     */
    @GetMapping("/check-name")
    public ApiResponse<java.util.Map<String, Boolean>> checkServiceName(
            @PathVariable Long businessId,
            @RequestParam String name,
            @RequestParam(required = false) Long excludeId) {
        boolean duplicate = serviceService.checkServiceNameDuplicate(businessId, name, excludeId);
        return ApiResponse.success(java.util.Map.of("duplicate", duplicate));
    }

    /**
     * Service 검색 (조건별)
     */
    @GetMapping("/search")
    public ApiResponse<List<ServiceResponse>> searchServices(
            @PathVariable Long businessId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Long staffId) {

        ServiceSearchCondition condition = ServiceSearchCondition.builder()
                .businessId(businessId)
                .categoryId(categoryId)
                .isActive(isActive)
                .staffId(staffId)
                .build();

        List<ServiceResponse> response = serviceService.searchServices(condition);
        return ApiResponse.success(response);
    }

    /**
     * Service 수정
     */
    @PatchMapping("/{serviceId}")
    public ApiResponse<ServiceResponse> updateService(
            @PathVariable Long businessId,
            @PathVariable Long serviceId,
            @Valid @RequestBody ServiceUpdateRequest request) {
        ServiceResponse response = serviceService.updateService(businessId, serviceId, request);
        return ApiResponse.success(response);
    }

    /**
     * Service 활성/비활성 전환
     */
    @PatchMapping("/{serviceId}/toggle-active")
    public ApiResponse<ServiceResponse> toggleServiceActive(
            @PathVariable Long businessId,
            @PathVariable Long serviceId) {
        ServiceResponse response = serviceService.toggleServiceActive(businessId, serviceId);
        return ApiResponse.success(response);
    }

    /**
     * Service 정렬 순서 변경
     */
    @PatchMapping("/sort-order")
    public ApiResponse<List<ServiceResponse>> updateServiceSortOrder(
            @PathVariable Long businessId,
            @Valid @RequestBody SortOrderUpdateRequest request) {
        List<ServiceResponse> response = serviceService.updateServiceSortOrder(businessId, request);
        return ApiResponse.success(response);
    }

    /**
     * Service 삭제
     */
    @DeleteMapping("/{serviceId}")
    public ApiResponse<Void> deleteService(
            @PathVariable Long businessId,
            @PathVariable Long serviceId) {
        serviceService.deleteService(businessId, serviceId);
        return ApiResponse.success();
    }
}
