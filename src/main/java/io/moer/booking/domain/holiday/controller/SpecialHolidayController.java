package io.moer.booking.domain.holiday.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.domain.holiday.dto.SpecialHolidayCreateRequest;
import io.moer.booking.domain.holiday.dto.SpecialHolidayResponse;
import io.moer.booking.domain.holiday.service.SpecialHolidayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/businesses/{businessId}/holidays")
@RequiredArgsConstructor
public class SpecialHolidayController {

    private final SpecialHolidayService holidayService;

    /**
     * 특별 휴무일 등록
     */
    @PostMapping
    public ApiResponse<SpecialHolidayResponse> createHoliday(
            @PathVariable Long businessId,
            @Valid @RequestBody SpecialHolidayCreateRequest request) {
        SpecialHolidayResponse response = holidayService.createHoliday(businessId, request);
        return ApiResponse.success(response);
    }

    /**
     * Business의 전체 휴무일 조회
     */
    @GetMapping
    public ApiResponse<List<SpecialHolidayResponse>> getHolidaysByBusiness(
            @PathVariable Long businessId) {
        List<SpecialHolidayResponse> response = holidayService.getHolidaysByBusiness(businessId);
        return ApiResponse.success(response);
    }

    /**
     * Business의 특정 기간 휴무일 조회
     */
    @GetMapping("/range")
    public ApiResponse<List<SpecialHolidayResponse>> getHolidaysByDateRange(
            @PathVariable Long businessId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<SpecialHolidayResponse> response =
                holidayService.getHolidaysByDateRange(businessId, startDate, endDate);
        return ApiResponse.success(response);
    }

    /**
     * 특정 날짜가 휴무일인지 확인
     */
    @GetMapping("/check")
    public ApiResponse<Boolean> isHoliday(
            @PathVariable Long businessId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        boolean isHoliday = holidayService.isHoliday(businessId, date);
        return ApiResponse.success(isHoliday);
    }

    /**
     * 특별 휴무일 삭제 (ID)
     */
    @DeleteMapping("/{holidayId}")
    public ApiResponse<Void> deleteHoliday(
            @PathVariable Long businessId,
            @PathVariable Long holidayId) {
        holidayService.deleteHoliday(businessId, holidayId);
        return ApiResponse.success();
    }

    /**
     * 특별 휴무일 삭제 (날짜)
     */
    @DeleteMapping("/date/{holidayDate}")
    public ApiResponse<Void> deleteHolidayByDate(
            @PathVariable Long businessId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate holidayDate) {
        holidayService.deleteHolidayByDate(businessId, holidayDate);
        return ApiResponse.success();
    }
}