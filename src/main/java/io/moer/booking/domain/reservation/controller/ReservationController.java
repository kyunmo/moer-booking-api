package io.moer.booking.domain.reservation.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.domain.reservation.ReservationStatus;
import io.moer.booking.domain.reservation.dto.ReservationCreateRequest;
import io.moer.booking.domain.reservation.dto.ReservationResponse;
import io.moer.booking.domain.reservation.dto.ReservationSearchCondition;
import io.moer.booking.domain.reservation.dto.ReservationUpdateRequest;
import io.moer.booking.domain.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 예약 관리 Controller
 * Business 기준 예약 CRUD
 */
@RestController
@RequestMapping("/api/businesses/{businessId}/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    // ========================================
    // 생성
    // ========================================

    /**
     * 예약 생성
     * POST /api/businesses/{businessId}/reservations
     */
    @PostMapping
    public ApiResponse<ReservationResponse> createReservation(
            @PathVariable Long businessId,
            @Valid @RequestBody ReservationCreateRequest request) {
        ReservationResponse response = reservationService.createReservation(businessId, request);
        return ApiResponse.success(response);
    }

    // ========================================
    // 조회
    // ========================================

    /**
     * 예약 단건 조회
     * GET /api/businesses/{businessId}/reservations/{reservationId}
     */
    @GetMapping("/{reservationId}")
    public ApiResponse<ReservationResponse> getReservation(
            @PathVariable Long businessId,
            @PathVariable Long reservationId) {
        ReservationResponse response = reservationService.getReservation(businessId, reservationId);
        return ApiResponse.success(response);
    }

    /**
     * Business의 전체 예약 조회
     * GET /api/businesses/{businessId}/reservations
     */
    @GetMapping
    public ApiResponse<List<ReservationResponse>> getReservationsByBusiness(
            @PathVariable Long businessId) {
        List<ReservationResponse> response = reservationService.getReservationsByBusiness(businessId);
        return ApiResponse.success(response);
    }

    /**
     * 날짜별 예약 조회
     * GET /api/businesses/{businessId}/reservations/date/{date}
     */
    @GetMapping("/date/{date}")
    public ApiResponse<List<ReservationResponse>> getReservationsByDate(
            @PathVariable Long businessId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        List<ReservationResponse> response = reservationService.getReservationsByDate(businessId, date);
        return ApiResponse.success(response);
    }

    /**
     * 예약 검색 (조건별)
     * GET /api/businesses/{businessId}/reservations/search
     */
    @GetMapping("/search")
    public ApiResponse<List<ReservationResponse>> searchReservations(
            @PathVariable Long businessId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        ReservationSearchCondition condition = ReservationSearchCondition.builder()
                .businessId(businessId)
                .customerId(customerId)
                .staffId(staffId)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        List<ReservationResponse> response = reservationService.searchReservations(condition);
        return ApiResponse.success(response);
    }

    // ========================================
    // 수정
    // ========================================

    /**
     * 예약 수정
     * PATCH /api/businesses/{businessId}/reservations/{reservationId}
     */
    @PatchMapping("/{reservationId}")
    public ApiResponse<ReservationResponse> updateReservation(
            @PathVariable Long businessId,
            @PathVariable Long reservationId,
            @Valid @RequestBody ReservationUpdateRequest request) {
        ReservationResponse response = reservationService.updateReservation(businessId, reservationId, request);
        return ApiResponse.success(response);
    }

    // ========================================
    // 상태 변경
    // ========================================

    /**
     * 예약 확정
     * PATCH /api/businesses/{businessId}/reservations/{reservationId}/confirm
     */
    @PatchMapping("/{reservationId}/confirm")
    public ApiResponse<ReservationResponse> confirmReservation(
            @PathVariable Long businessId,
            @PathVariable Long reservationId) {
        ReservationResponse response = reservationService.confirmReservation(businessId, reservationId);
        return ApiResponse.success(response);
    }

    /**
     * 예약 완료
     * PATCH /api/businesses/{businessId}/reservations/{reservationId}/complete
     */
    @PatchMapping("/{reservationId}/complete")
    public ApiResponse<ReservationResponse> completeReservation(
            @PathVariable Long businessId,
            @PathVariable Long reservationId) {
        ReservationResponse response = reservationService.completeReservation(businessId, reservationId);
        return ApiResponse.success(response);
    }

    /**
     * 예약 취소
     * PATCH /api/businesses/{businessId}/reservations/{reservationId}/cancel
     */
    @PatchMapping("/{reservationId}/cancel")
    public ApiResponse<ReservationResponse> cancelReservation(
            @PathVariable Long businessId,
            @PathVariable Long reservationId,
            @RequestParam(required = false, defaultValue = "") String reason) {
        ReservationResponse response = reservationService.cancelReservation(businessId, reservationId, reason);
        return ApiResponse.success(response);
    }

    /**
     * 노쇼 처리
     * PATCH /api/businesses/{businessId}/reservations/{reservationId}/no-show
     */
    @PatchMapping("/{reservationId}/no-show")
    public ApiResponse<ReservationResponse> markAsNoShow(
            @PathVariable Long businessId,
            @PathVariable Long reservationId) {
        ReservationResponse response = reservationService.markAsNoShow(businessId, reservationId);
        return ApiResponse.success(response);
    }

    // ========================================
    // 삭제
    // ========================================

    /**
     * 예약 삭제
     * DELETE /api/businesses/{businessId}/reservations/{reservationId}
     */
    @DeleteMapping("/{reservationId}")
    public ApiResponse<Void> deleteReservation(
            @PathVariable Long businessId,
            @PathVariable Long reservationId) {
        reservationService.deleteReservation(businessId, reservationId);
        return ApiResponse.success();
    }
}