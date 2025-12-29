package io.moer.booking.domain.reservation.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.domain.reservation.dto.ReservationResponse;
import io.moer.booking.domain.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReservationQueryController {

    private final ReservationService reservationService;

    /**
     * Customer의 예약 조회
     */
    @GetMapping("/customers/{customerId}/reservations")
    public ApiResponse<List<ReservationResponse>> getReservationsByCustomer(
            @PathVariable Long customerId) {
        List<ReservationResponse> response = reservationService.getReservationsByCustomer(customerId);
        return ApiResponse.success(response);
    }

    /**
     * Staff의 예약 조회
     */
    @GetMapping("/staffs/{staffId}/reservations")
    public ApiResponse<List<ReservationResponse>> getReservationsByStaff(
            @PathVariable Long staffId) {
        List<ReservationResponse> response = reservationService.getReservationsByStaff(staffId);
        return ApiResponse.success(response);
    }

    /**
     * 예약 번호로 조회 (고객용)
     */
    @GetMapping("/reservations/number/{reservationNumber}")
    public ApiResponse<ReservationResponse> getReservationByNumber(
            @PathVariable String reservationNumber) {
        ReservationResponse response = reservationService.getReservationByNumber(reservationNumber);
        return ApiResponse.success(response);
    }
}