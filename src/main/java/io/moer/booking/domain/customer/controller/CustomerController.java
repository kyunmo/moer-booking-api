package io.moer.booking.domain.customer.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.customer.Customer;
import io.moer.booking.domain.customer.dto.*;
import io.moer.booking.domain.customer.service.CustomerService;
import io.moer.booking.domain.subscription.service.SubscriptionCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/businesses/{businessId}/customers")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "고객 관리 API")
public class CustomerController {

    private final CustomerService customerService;
    private final SubscriptionCheckService subscriptionCheckService;

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
        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);
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
     * 고객 세그멘테이션 조회
     */
    @GetMapping("/segments")
    @Operation(summary = "고객 세그멘테이션 조회", description = "VIP/INACTIVE/BIRTHDAY/NEW/FREQUENT 타입별 고객 세그먼트를 조회합니다")
    public ApiResponse<CustomerSegmentResponse> getCustomerSegment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String type) {
        Long businessId = userDetails.getUser().getBusinessId();
        CustomerSegmentResponse response = customerService.getCustomerSegment(businessId, type);
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

    // ========================================
    // 고객 메모 CRUD
    // ========================================

    /**
     * 고객 메모 생성
     */
    @PostMapping("/{customerId}/notes")
    @Operation(summary = "고객 메모 생성", description = "특정 고객에 대한 메모를 생성합니다.")
    public ApiResponse<CustomerNoteResponse> createNote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerNoteRequest request) {
        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);
        CustomerNoteResponse response = customerService.createNote(
                businessId, customerId, request,
                userDetails.getUser().getId(), userDetails.getUser().getName());
        return ApiResponse.success(response);
    }

    /**
     * 고객 메모 목록 조회
     */
    @GetMapping("/{customerId}/notes")
    @Operation(summary = "고객 메모 목록 조회", description = "특정 고객의 메모 목록을 조회합니다.")
    public ApiResponse<List<CustomerNoteResponse>> getNotes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @PathVariable Long customerId) {
        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);
        List<CustomerNoteResponse> response = customerService.getNotes(businessId, customerId);
        return ApiResponse.success(response);
    }

    /**
     * 고객 메모 수정
     */
    @PutMapping("/{customerId}/notes/{noteId}")
    @Operation(summary = "고객 메모 수정", description = "고객 메모를 수정합니다.")
    public ApiResponse<CustomerNoteResponse> updateNote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @PathVariable Long customerId,
            @PathVariable Long noteId,
            @Valid @RequestBody CustomerNoteRequest request) {
        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);
        CustomerNoteResponse response = customerService.updateNote(businessId, customerId, noteId, request);
        return ApiResponse.success(response);
    }

    /**
     * 고객 메모 삭제
     */
    @DeleteMapping("/{customerId}/notes/{noteId}")
    @Operation(summary = "고객 메모 삭제", description = "고객 메모를 삭제합니다.")
    public ApiResponse<Void> deleteNote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @PathVariable Long customerId,
            @PathVariable Long noteId) {
        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);
        customerService.deleteNote(businessId, customerId, noteId);
        return ApiResponse.success();
    }

    // ========================================
    // 고객 태그 관리
    // ========================================

    /**
     * 고객 태그 수정
     */
    @PutMapping("/{customerId}/tags")
    @Operation(summary = "고객 태그 수정", description = "고객의 태그를 수정합니다. 최대 10개, 각 20자 이내.")
    public ApiResponse<CustomerResponse> updateTags(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerTagRequest request) {
        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);
        CustomerResponse response = customerService.updateTags(businessId, customerId, request);
        return ApiResponse.success(response);
    }

    /**
     * 매장의 모든 고유 태그 목록 조회
     */
    @GetMapping("/tags")
    @Operation(summary = "매장 태그 목록 조회", description = "매장 고객들에게 사용된 모든 고유 태그를 조회합니다.")
    public ApiResponse<CustomerTagResponse> getAllTags(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId) {
        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);
        CustomerTagResponse response = customerService.getAllTags(businessId);
        return ApiResponse.success(response);
    }

    // ========================================
    // 고객 CSV 내보내기
    // ========================================

    /**
     * 고객 CSV 내보내기
     */
    @GetMapping("/export")
    @Operation(summary = "고객 CSV 내보내기", description = "고객 목록을 CSV 파일로 내보냅니다. BASIC 플랜 이상 필요.")
    public void exportCustomersCsv(
            @PathVariable Long businessId,
            @RequestParam(required = false) String segment,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse response) throws IOException {
        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);
        // BASIC 플랜 이상 체크
        subscriptionCheckService.checkPremiumAccess(businessId);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"customers_" + LocalDate.now() + ".csv\"");

        // UTF-8 BOM (엑셀 한글 깨짐 방지)
        OutputStream os = response.getOutputStream();
        os.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        writer.println("이름,전화번호,이메일,등급,총 방문 횟수,총 이용 금액,최근 방문일,가입일,태그");

        List<Customer> customers = customerService.getCustomersForExport(businessId, segment, tags, startDate, endDate);
        for (Customer c : customers) {
            writer.println(String.format("%s,%s,%s,%s,%d,%d,%s,%s,\"%s\"",
                    nullSafe(c.getName()),
                    nullSafe(c.getPhone()),
                    nullSafe(c.getEmail()),
                    getSegmentLabel(c),
                    c.getVisitCount() != null ? c.getVisitCount() : 0,
                    c.getTotalSpent() != null ? c.getTotalSpent() : 0,
                    nullSafe(c.getLastVisitDate()),
                    nullSafe(c.getCreatedAt()),
                    nullSafe(c.getTags())));
        }
        writer.flush();
    }

    // ========================================
    // 고객 중복 감지 및 병합
    // ========================================

    /**
     * 중복 고객 감지
     */
    @GetMapping("/duplicates")
    @Operation(summary = "중복 고객 감지", description = "전화번호 기반으로 중복된 고객을 감지합니다.")
    public ApiResponse<List<DuplicateCustomerResponse>> findDuplicates(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId) {
        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);
        List<DuplicateCustomerResponse> response = customerService.findDuplicates(businessId);
        return ApiResponse.success(response);
    }

    /**
     * 고객 병합
     */
    @PostMapping("/merge")
    @Operation(summary = "고객 병합", description = "중복 고객을 병합합니다. 예약, 메모, 태그, 통계가 주 고객으로 이관됩니다.")
    public ApiResponse<CustomerMergeResponse> mergeCustomers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @Valid @RequestBody CustomerMergeRequest request) {
        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);
        CustomerMergeResponse response = customerService.mergeCustomers(businessId, request);
        return ApiResponse.success(response);
    }

    // ========================================
    // Private 헬퍼
    // ========================================

    private String nullSafe(Object value) {
        return value != null ? value.toString() : "";
    }

    private String getSegmentLabel(Customer customer) {
        if (customer.isVip()) return "VIP";
        if (customer.isRegular()) return "단골";
        if (customer.isNew()) return "신규";
        return "일반";
    }
}