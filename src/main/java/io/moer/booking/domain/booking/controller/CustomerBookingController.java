package io.moer.booking.domain.booking.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.booking.dto.CustomerReservationCancelRequest;
import io.moer.booking.domain.booking.dto.CustomerReservationCreateRequest;
import io.moer.booking.domain.booking.dto.CustomerReservationListResponse;
import io.moer.booking.domain.booking.dto.PublicReservationResponse;
import io.moer.booking.domain.booking.service.CustomerBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 로그인 고객용 예약 API Controller
 * JWT 인증이 필요하며, userId 기반으로 본인 확인이 이루어집니다.
 */
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@Tag(name = "Customer Booking", description = "로그인 고객용 예약 관리 API")
public class CustomerBookingController {

    private final CustomerBookingService customerBookingService;

    /**
     * 로그인 고객 예약 생성
     * User 정보에서 이름/전화번호/이메일이 자동으로 사용됩니다.
     */
    @PostMapping("/businesses/{slug}/reservations")
    @Operation(summary = "예약 생성", description = "로그인 고객이 매장에 예약을 생성합니다")
    public ResponseEntity<ApiResponse<PublicReservationResponse>> createReservation(
            @Parameter(description = "매장 슬러그") @PathVariable String slug,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CustomerReservationCreateRequest request) {
        PublicReservationResponse response = customerBookingService.createReservation(
                slug, userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 내 예약 목록 조회
     * 로그인 사용자의 모든 예약을 상태별 필터와 페이징으로 조회합니다.
     */
    @GetMapping("/reservations")
    @Operation(summary = "내 예약 목록 조회", description = "로그인 고객의 예약 목록을 조회합니다")
    public ResponseEntity<ApiResponse<PageResponse<CustomerReservationListResponse>>> getMyReservations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "예약 상태 필터 (PENDING, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW)")
            @RequestParam(required = false) String status,
            @Parameter(description = "페이지 번호 (1부터 시작)")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지당 개수")
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<CustomerReservationListResponse> response =
                customerBookingService.getMyReservations(userDetails.getUserId(), status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 예약 상세 조회
     * 예약 번호로 본인의 예약 상세 정보를 조회합니다.
     */
    @GetMapping("/reservations/{reservationNumber}")
    @Operation(summary = "예약 상세 조회", description = "예약 번호로 예약 상세를 조회합니다")
    public ResponseEntity<ApiResponse<CustomerReservationListResponse>> getReservation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "예약 번호") @PathVariable String reservationNumber) {
        CustomerReservationListResponse response =
                customerBookingService.getReservation(userDetails.getUserId(), reservationNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 예약 취소
     * userId 기반 본인 확인이 이루어지므로 전화번호 검증이 불필요합니다.
     */
    @PostMapping("/reservations/{reservationNumber}/cancel")
    @Operation(summary = "예약 취소", description = "예약을 취소합니다")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "예약 번호") @PathVariable String reservationNumber,
            @RequestBody(required = false) CustomerReservationCancelRequest request) {
        customerBookingService.cancelReservation(userDetails.getUserId(), reservationNumber, request);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
