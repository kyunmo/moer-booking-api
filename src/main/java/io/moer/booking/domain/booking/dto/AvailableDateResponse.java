package io.moer.booking.domain.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 예약 가능 날짜 응답 DTO (Public API)
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "예약 가능 날짜 목록")
public class AvailableDateResponse {

    @Schema(description = "조회 년월", example = "2026-02")
    private String month;

    @Schema(description = "날짜별 예약 가능 여부 목록")
    private List<DateSlot> availableDates;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "날짜별 예약 가능 여부")
    public static class DateSlot {

        @Schema(description = "날짜", example = "2026-02-20")
        private LocalDate date;

        @Schema(description = "예약 가능 슬롯 존재 여부")
        private boolean hasSlots;
    }
}
