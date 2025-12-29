package io.moer.booking.domain.customer.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.domain.customer.dto.CustomerHistoryCreateRequest;
import io.moer.booking.domain.customer.dto.CustomerHistoryResponse;
import io.moer.booking.domain.customer.service.CustomerHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/businesses/{businessId}/histories")
@RequiredArgsConstructor
public class CustomerHistoryController {

    private final CustomerHistoryService historyService;

    /**
     * 이력 생성 (수동)
     */
    @PostMapping
    public ApiResponse<CustomerHistoryResponse> createHistory(
            @PathVariable Long businessId,
            @Valid @RequestBody CustomerHistoryCreateRequest request) {
        CustomerHistoryResponse response = historyService.createHistory(businessId, request);
        return ApiResponse.success(response);
    }

    /**
     * 이력 단건 조회
     */
    @GetMapping("/{historyId}")
    public ApiResponse<CustomerHistoryResponse> getHistory(
            @PathVariable Long businessId,
            @PathVariable Long historyId) {
        CustomerHistoryResponse response = historyService.getHistory(businessId, historyId);
        return ApiResponse.success(response);
    }

    /**
     * Business의 전체 이력 조회
     */
    @GetMapping
    public ApiResponse<List<CustomerHistoryResponse>> getHistoriesByBusiness(
            @PathVariable Long businessId) {
        List<CustomerHistoryResponse> response = historyService.getHistoriesByBusiness(businessId);
        return ApiResponse.success(response);
    }

    /**
     * Customer의 이력 조회
     */
    @GetMapping("/customer/{customerId}")
    public ApiResponse<List<CustomerHistoryResponse>> getHistoriesByCustomer(
            @PathVariable Long businessId,
            @PathVariable Long customerId) {
        List<CustomerHistoryResponse> response = historyService.getHistoriesByCustomer(customerId);
        return ApiResponse.success(response);
    }

    /**
     * Customer의 기간별 이력 조회
     */
    @GetMapping("/customer/{customerId}/range")
    public ApiResponse<List<CustomerHistoryResponse>> getHistoriesByDateRange(
            @PathVariable Long businessId,
            @PathVariable Long customerId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        List<CustomerHistoryResponse> response = historyService.getHistoriesByDateRange(
                customerId, startDate, endDate);
        return ApiResponse.success(response);
    }

    /**
     * Customer의 최근 방문 이력 조회
     */
    @GetMapping("/customer/{customerId}/latest")
    public ApiResponse<CustomerHistoryResponse> getLatestHistory(
            @PathVariable Long businessId,
            @PathVariable Long customerId) {
        CustomerHistoryResponse response = historyService.getLatestHistory(customerId);
        return ApiResponse.success(response);
    }

    /**
     * 이력 수정
     */
    @PatchMapping("/{historyId}")
    public ApiResponse<CustomerHistoryResponse> updateHistory(
            @PathVariable Long businessId,
            @PathVariable Long historyId,
            @Valid @RequestBody CustomerHistoryCreateRequest request) {
        CustomerHistoryResponse response = historyService.updateHistory(businessId, historyId, request);
        return ApiResponse.success(response);
    }

    /**
     * 이력 삭제
     */
    @DeleteMapping("/{historyId}")
    public ApiResponse<Void> deleteHistory(
            @PathVariable Long businessId,
            @PathVariable Long historyId) {
        historyService.deleteHistory(businessId, historyId);
        return ApiResponse.success();
    }
}