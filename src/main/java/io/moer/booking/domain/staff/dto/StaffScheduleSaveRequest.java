package io.moer.booking.domain.staff.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

/**
 * 직원 근무 스케줄 일괄 저장 요청 DTO
 * 7일분(월~일)을 한 번에 저장
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StaffScheduleSaveRequest {

    @NotNull(message = "스케줄 목록은 필수입니다")
    @Size(min = 7, max = 7, message = "7일분의 스케줄이 필요합니다")
    @Valid
    private List<DaySchedule> schedules;

    /**
     * 요일별 스케줄
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DaySchedule {

        @NotNull(message = "요일은 필수입니다")
        @Min(value = 1, message = "요일은 1(월) 이상이어야 합니다")
        @Max(value = 7, message = "요일은 7(일) 이하여야 합니다")
        private Integer dayOfWeek;

        private LocalTime startTime;
        private LocalTime endTime;
        private LocalTime breakStartTime;
        private LocalTime breakEndTime;

        @NotNull(message = "근무 여부는 필수입니다")
        private Boolean isWorking;
    }
}
