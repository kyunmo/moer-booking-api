package io.moer.booking.domain.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 예약 가능 시간 슬롯 응답 DTO (Public API)
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "예약 가능 시간 슬롯 목록")
public class AvailableTimeSlotResponse {

    @Schema(description = "조회 날짜", example = "2026-02-20")
    private LocalDate date;

    @Schema(description = "서비스 소요 시간 (분)", example = "60")
    private int serviceDuration;

    @Schema(description = "예약 가능 시간 슬롯 목록")
    private List<TimeSlot> availableSlots;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "예약 가능 시간 슬롯")
    public static class TimeSlot {

        @JsonFormat(pattern = "HH:mm")
        @Schema(description = "시작 시간", example = "14:00")
        private LocalTime startTime;

        @JsonFormat(pattern = "HH:mm")
        @Schema(description = "종료 시간", example = "15:00")
        private LocalTime endTime;

        @Schema(description = "해당 시간에 가용한 스태프 목록")
        private List<StaffInfo> availableStaffs;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "스태프 정보")
    public static class StaffInfo {

        @Schema(description = "스태프 ID", example = "1")
        private Long id;

        @Schema(description = "스태프 이름", example = "김미소")
        private String name;
    }
}
