package io.moer.booking.domain.staff.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * 예약 가능한 시간 슬롯
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableTimeSlot {

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;
}
