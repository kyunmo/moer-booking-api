package io.moer.booking.domain.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 직원 근무 스케줄 엔티티
 * DB 테이블: staff_schedules
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffSchedule {
    private Long id;
    private Long staffId;
    private Long businessId;

    /**
     * 요일 (ISO-8601: 1=월, 2=화, ..., 7=일)
     */
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalTime breakStartTime;
    private LocalTime breakEndTime;

    /**
     * 근무 여부 (Y/N)
     * DB: CHAR(1)
     */
    private String isWorking;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 근무일 여부 확인
     */
    public boolean isWorkingDay() {
        return "Y".equals(this.isWorking);
    }

    /**
     * 휴식 시간이 설정되어 있는지 확인
     */
    public boolean hasBreakTime() {
        return breakStartTime != null && breakEndTime != null;
    }
}
