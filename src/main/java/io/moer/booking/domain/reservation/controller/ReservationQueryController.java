package io.moer.booking.domain.reservation.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.domain.reservation.dto.ReservationResponse;
import io.moer.booking.domain.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 예약 조회 전용 Controller
 * Customer/Staff 기준 예약 조회
 */
@RestController
@RequiredArgsConstructor
public class ReservationQueryController {

    private final ReservationService reservationService;

    /**
     * Customer의 예약 조회
     * GET /api/customers/{customerId}/reservations
     */
    @GetMapping("/api/customers/{customerId}/reservations")
    public ApiResponse<List<ReservationResponse>> getReservationsByCustomer(
            @PathVariable Long customerId) {
        List<ReservationResponse> response = reservationService.getReservationsByCustomer(customerId);
        return ApiResponse.success(response);
    }

    /**
     * Staff의 예약 조회
     * GET /api/staffs/{staffId}/reservations
     */
    @GetMapping("/api/staffs/{staffId}/reservations")
    public ApiResponse<List<ReservationResponse>> getReservationsByStaff(
            @PathVariable Long staffId) {
        List<ReservationResponse> response = reservationService.getReservationsByStaff(staffId);
        return ApiResponse.success(response);
    }

    /**
     * 예약 번호로 조회 (고객용)
     * GET /api/reservations/number/{reservationNumber}
     */
    @GetMapping("/api/reservations/number/{reservationNumber}")
    public ApiResponse<ReservationResponse> getReservationByNumber(
            @PathVariable String reservationNumber) {
        ReservationResponse response = reservationService.getReservationByNumber(reservationNumber);
        return ApiResponse.success(response);
    }
}