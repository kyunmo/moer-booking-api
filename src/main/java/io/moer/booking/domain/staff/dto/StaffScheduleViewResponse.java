package io.moer.booking.domain.staff.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 직원 주간 스케줄 조회 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffScheduleViewResponse {

    private Long staffId;
    private String staffName;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<ScheduleReservation> reservations;
    private List<DaySchedule> workSchedule;
    private List<BlockedSlot> blockedSlots;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScheduleReservation {
        private Long id;
        private String reservationNumber;
        private String customerName;
        private String customerPhone;
        private String serviceName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DaySchedule {
        private LocalDate date;
        private String dayOfWeek;
        private boolean isWorkDay;
        private LocalTime workStartTime;
        private LocalTime workEndTime;
        private LocalTime breakStartTime;
        private LocalTime breakEndTime;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BlockedSlot {
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private String reason;
    }
}
