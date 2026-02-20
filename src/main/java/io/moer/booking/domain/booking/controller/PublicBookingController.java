package io.moer.booking.domain.booking.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.domain.booking.dto.*;
import io.moer.booking.domain.booking.service.PublicBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 고객용 예약 Public API
 * 인증 없이 접근 가능
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Public Booking", description = "고객용 예약 API (비인증)")
public class PublicBookingController {

    private final PublicBookingService publicBookingService;

    /**
     * 매장 휴무일 목록 조회
     */
    @GetMapping("/businesses/{slug}/holidays")
    @Operation(
            summary = "매장 휴무일 조회",
            description = "매장의 휴무일 목록을 조회합니다. year 파라미터로 연도 필터링이 가능합니다."
    )
    public ApiResponse<List<PublicHolidayResponse>> getHolidays(
            @Parameter(description = "매장 슬러그", required = true) @PathVariable String slug,
            @Parameter(description = "조회 연도 (선택, 미지정 시 전체)") @RequestParam(required = false) Integer year) {

        List<PublicHolidayResponse> response = publicBookingService.getHolidays(slug, year);
        return ApiResponse.success(response);
    }

    /**
     * 예약 가능 날짜 조회
     */
    @GetMapping("/businesses/{slug}/available-dates")
    @Operation(
            summary = "예약 가능 날짜 조회",
            description = "해당 월의 예약 가능 날짜 목록을 조회합니다. 스태프/서비스 선택 시 해당 조건에 맞는 날짜만 반환합니다."
    )
    public ApiResponse<AvailableDateResponse> getAvailableDates(
            @Parameter(description = "매장 슬러그", required = true) @PathVariable String slug,
            @Parameter(description = "스태프 ID (선택)") @RequestParam(required = false) Long staffId,
            @Parameter(description = "서비스 ID (선택)") @RequestParam(required = false) Long serviceId,
            @Parameter(description = "조회 년월 (예: 2026-02)", required = true) @RequestParam String month) {

        YearMonth yearMonth = YearMonth.parse(month);
        AvailableDateResponse response = publicBookingService.getAvailableDates(slug, staffId, serviceId, yearMonth);
        return ApiResponse.success(response);
    }

    /**
     * 예약 가능 시간 조회
     */
    @GetMapping("/businesses/{slug}/available-times")
    @Operation(
            summary = "예약 가능 시간 조회",
            description = "특정 날짜의 예약 가능 시간 슬롯을 조회합니다. 서비스 ID는 필수이며, 스태프 미지정 시 모든 가용 스태프의 슬롯을 반환합니다."
    )
    public ApiResponse<AvailableTimeSlotResponse> getAvailableTimes(
            @Parameter(description = "매장 슬러그", required = true) @PathVariable String slug,
            @Parameter(description = "예약 날짜", required = true) @RequestParam LocalDate date,
            @Parameter(description = "서비스 ID", required = true) @RequestParam Long serviceId,
            @Parameter(description = "스태프 ID (선택)") @RequestParam(required = false) Long staffId) {

        AvailableTimeSlotResponse response = publicBookingService.getAvailableTimes(slug, date, serviceId, staffId);
        return ApiResponse.success(response);
    }

    /**
     * 고객 예약 생성
     */
    @PostMapping("/businesses/{slug}/reservations")
    @Operation(
            summary = "고객 예약 생성",
            description = "고객이 직접 예약을 생성합니다. 예약번호와 상태가 반환됩니다."
    )
    public ResponseEntity<ApiResponse<PublicReservationResponse>> createReservation(
            @Parameter(description = "매장 슬러그", required = true) @PathVariable String slug,
            @Valid @RequestBody PublicReservationCreateRequest request) {

        PublicReservationResponse response = publicBookingService.createReservation(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 이름+전화번호 기반 예약 조회
     */
    @GetMapping("/reservations/lookup")
    @Operation(
            summary = "이름+전화번호 기반 예약 조회",
            description = "이름과 전화번호로 해당 고객의 모든 예약 목록을 조회합니다. 완료/취소된 예약도 포함됩니다."
    )
    public ApiResponse<List<PublicReservationLookupResponse>> lookupReservations(
            @Parameter(description = "고객 이름", required = true) @RequestParam String name,
            @Parameter(description = "고객 전화번호", required = true) @RequestParam String phone) {

        List<PublicReservationLookupResponse> response = publicBookingService.lookupReservations(name, phone);
        return ApiResponse.success(response);
    }

    /**
     * 예약 조회 (예약번호 + 전화번호)
     */
    @GetMapping("/reservations/{reservationNumber}")
    @Operation(
            summary = "예약 조회 (예약번호+전화번호)",
            description = "예약번호와 전화번호로 예약 상세 정보를 조회합니다. 본인 확인용."
    )
    public ApiResponse<PublicReservationDetailResponse> getReservation(
            @Parameter(description = "예약번호", required = true) @PathVariable String reservationNumber,
            @Parameter(description = "본인 확인용 전화번호", required = true) @RequestParam String phone) {

        PublicReservationDetailResponse response = publicBookingService.getReservation(reservationNumber, phone);
        return ApiResponse.success(response);
    }

    /**
     * 예약 취소
     */
    @PostMapping("/reservations/{reservationNumber}/cancel")
    @Operation(
            summary = "예약 취소",
            description = "예약번호와 전화번호로 본인 확인 후 예약을 취소합니다."
    )
    public ApiResponse<Void> cancelReservation(
            @Parameter(description = "예약번호", required = true) @PathVariable String reservationNumber,
            @Valid @RequestBody PublicReservationCancelRequest request) {

        publicBookingService.cancelReservation(reservationNumber, request);
        return ApiResponse.success();
    }
}
