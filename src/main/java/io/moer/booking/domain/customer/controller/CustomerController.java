package io.moer.booking.domain.customer.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.customer.dto.*;
import io.moer.booking.domain.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/businesses/{businessId}/customers")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "고객 관리 API")
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Customer 생성
     */
    @PostMapping
    public ApiResponse<CustomerResponse> createCustomer(
            @PathVariable Long businessId,
            @Valid @RequestBody CustomerCreateRequest request) {
        CustomerResponse response = customerService.createCustomer(businessId, request);
        return ApiResponse.success(response);
    }

    /**
     * Customer 단건 조회
     */
    @GetMapping("/{customerId}")
    public ApiResponse<CustomerResponse> getCustomer(
            @PathVariable Long businessId,
            @PathVariable Long customerId) {
        CustomerResponse response = customerService.getCustomer(businessId, customerId);
        return ApiResponse.success(response);
    }

    /**
     * 고객 예약 이력 조회
     */
    @Operation(summary = "고객 예약 이력 조회", description = "특정 고객의 과거 예약 목록과 요약 통계를 조회합니다.")
    @GetMapping("/{customerId}/reservations")
    public ApiResponse<CustomerReservationHistoryResponse> getCustomerReservationHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        userDetails.getUser().canAccessBusiness(businessId);
        return ApiResponse.success(
                customerService.getCustomerReservationHistory(businessId, customerId, status, page, size));
    }

    /**
     * 전화번호로 Customer 조회
     */
    @GetMapping("/phone/{phone}")
    public ApiResponse<CustomerResponse> getCustomerByPhone(
            @PathVariable Long businessId,
            @PathVariable String phone) {
        CustomerResponse response = customerService.getCustomerByPhone(businessId, phone);
        return ApiResponse.success(response);
    }

    /**
     * Business의 전체 Customer 목록 조회
     */
    @GetMapping
    public ApiResponse<List<CustomerResponse>> getCustomersByBusiness(
            @PathVariable Long businessId) {
        List<CustomerResponse> response = customerService.getCustomersByBusiness(businessId);
        return ApiResponse.success(response);
    }

    /**
     * Customer 검색 (조건별)
     */
    @GetMapping("/search")
    public ApiResponse<List<CustomerResponse>> searchCustomers(
            @PathVariable Long businessId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Integer minVisitCount,
            @RequestParam(required = false, defaultValue = "visit_count") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {

        CustomerSearchCondition condition = CustomerSearchCondition.builder()
                .businessId(businessId)
                .name(name)
                .phone(phone)
                .tag(tag)
                .minVisitCount(minVisitCount)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .build();

        List<CustomerResponse> response = customerService.searchCustomers(condition);
        return ApiResponse.success(response);
    }

    /**
     * VIP 고객 목록 조회
     */
    @GetMapping("/vip")
    public ApiResponse<List<CustomerResponse>> getVipCustomers(
            @PathVariable Long businessId) {
        List<CustomerResponse> response = customerService.getVipCustomers(businessId);
        return ApiResponse.success(response);
    }

    /**
     * 신규 고객 목록 조회
     */
    @GetMapping("/new")
    public ApiResponse<List<CustomerResponse>> getNewCustomers(
            @PathVariable Long businessId) {
        List<CustomerResponse> response = customerService.getNewCustomers(businessId);
        return ApiResponse.success(response);
    }

    /**
     * 단골 고객 목록 조회
     */
    @GetMapping("/regular")
    public ApiResponse<List<CustomerResponse>> getRegularCustomers(
            @PathVariable Long businessId) {
        List<CustomerResponse> response = customerService.getRegularCustomers(businessId);
        return ApiResponse.success(response);
    }

    /**
     * Customer 수정
     */
    @PatchMapping("/{customerId}")
    public ApiResponse<CustomerResponse> updateCustomer(
            @PathVariable Long businessId,
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerUpdateRequest request) {
        CustomerResponse response = customerService.updateCustomer(businessId, customerId, request);
        return ApiResponse.success(response);
    }

    /**
     * Customer 삭제
     */
    @DeleteMapping("/{customerId}")
    public ApiResponse<Void> deleteCustomer(
            @PathVariable Long businessId,
            @PathVariable Long customerId) {
        customerService.deleteCustomer(businessId, customerId);
        return ApiResponse.success();
    }
}