package io.moer.booking.domain.service.category.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.domain.service.category.dto.ServiceCategoryCreateRequest;
import io.moer.booking.domain.service.category.dto.ServiceCategoryResponse;
import io.moer.booking.domain.service.category.dto.ServiceCategoryUpdateRequest;
import io.moer.booking.domain.service.category.dto.SortOrderUpdateRequest;
import io.moer.booking.domain.service.category.service.ServiceCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/businesses/{businessId}/service-categories")
@RequiredArgsConstructor
public class ServiceCategoryController {

    private final ServiceCategoryService serviceCategoryService;

    /**
     * 카테고리 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ServiceCategoryResponse>> createCategory(
            @PathVariable Long businessId,
            @Valid @RequestBody ServiceCategoryCreateRequest request) {
        ServiceCategoryResponse response = serviceCategoryService.createCategory(businessId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 카테고리 목록 조회 (sort_order 순)
     */
    @GetMapping
    public ApiResponse<List<ServiceCategoryResponse>> getCategoriesByBusiness(
            @PathVariable Long businessId) {
        List<ServiceCategoryResponse> response = serviceCategoryService.getCategoriesByBusiness(businessId);
        return ApiResponse.success(response);
    }

    /**
     * 카테고리 단건 조회
     */
    @GetMapping("/{categoryId}")
    public ApiResponse<ServiceCategoryResponse> getCategory(
            @PathVariable Long businessId,
            @PathVariable Long categoryId) {
        ServiceCategoryResponse response = serviceCategoryService.getCategory(businessId, categoryId);
        return ApiResponse.success(response);
    }

    /**
     * 카테고리 수정
     */
    @PatchMapping("/{categoryId}")
    public ApiResponse<ServiceCategoryResponse> updateCategory(
            @PathVariable Long businessId,
            @PathVariable Long categoryId,
            @Valid @RequestBody ServiceCategoryUpdateRequest request) {
        ServiceCategoryResponse response = serviceCategoryService.updateCategory(businessId, categoryId, request);
        return ApiResponse.success(response);
    }

    /**
     * 카테고리 삭제
     */
    @DeleteMapping("/{categoryId}")
    public ApiResponse<Void> deleteCategory(
            @PathVariable Long businessId,
            @PathVariable Long categoryId) {
        serviceCategoryService.deleteCategory(businessId, categoryId);
        return ApiResponse.success();
    }

    /**
     * 카테고리 정렬 순서 변경
     */
    @PatchMapping("/sort-order")
    public ApiResponse<List<ServiceCategoryResponse>> updateSortOrder(
            @PathVariable Long businessId,
            @Valid @RequestBody SortOrderUpdateRequest request) {
        List<ServiceCategoryResponse> response = serviceCategoryService.updateSortOrder(businessId, request);
        return ApiResponse.success(response);
    }
}
