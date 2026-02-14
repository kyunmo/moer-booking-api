package io.moer.booking.domain.staff.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 직원 가용 시간 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffAvailableTimesResponse {
    private Long staffId;
    private String staffName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private String dayName;
    private Boolean isWorkingDay;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime workStart;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime workEnd;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime breakStart;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime breakEnd;

    private List<BookedSlot> bookedSlots;
    private List<AvailableTimeSlot> availableSlots;

    /**
     * 예약된 시간 슬롯
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookedSlot {
        @JsonFormat(pattern = "HH:mm")
        private LocalTime start;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime end;

        private Long reservationId;
    }
}
