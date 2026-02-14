package io.moer.booking.domain.staff.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.staff.StaffSchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * 직원 근무 스케줄 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffScheduleResponse {
    private Long id;
    private Long staffId;
    private Integer dayOfWeek;
    private String dayName;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime breakStartTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime breakEndTime;

    private Boolean isWorking;

    /**
     * Entity -> DTO 변환
     */
    public static StaffScheduleResponse from(StaffSchedule schedule) {
        return StaffScheduleResponse.builder()
                .id(schedule.getId())
                .staffId(schedule.getStaffId())
                .dayOfWeek(schedule.getDayOfWeek())
                .dayName(getDayNameFromNumber(schedule.getDayOfWeek()))
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .breakStartTime(schedule.getBreakStartTime())
                .breakEndTime(schedule.getBreakEndTime())
                .isWorking("Y".equals(schedule.getIsWorking()))
                .build();
    }

    /**
     * ISO-8601 요일 번호 -> 한글 요일명 변환
     */
    private static String getDayNameFromNumber(Integer dayOfWeek) {
        if (dayOfWeek == null) return null;
        return switch (dayOfWeek) {
            case 1 -> "월";
            case 2 -> "화";
            case 3 -> "수";
            case 4 -> "목";
            case 5 -> "금";
            case 6 -> "토";
            case 7 -> "일";
            default -> null;
        };
    }
}
