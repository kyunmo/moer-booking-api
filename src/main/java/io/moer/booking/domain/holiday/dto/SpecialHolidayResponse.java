package io.moer.booking.domain.holiday.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.holiday.SpecialHoliday;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class SpecialHolidayResponse {

    private Long id;
    private Long businessId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate holidayDate;

    private String reason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public static SpecialHolidayResponse from(SpecialHoliday holiday) {
        return SpecialHolidayResponse.builder()
                .id(holiday.getId())
                .businessId(holiday.getBusinessId())
                .holidayDate(holiday.getHolidayDate())
                .reason(holiday.getReason())
                .createdAt(holiday.getCreatedAt())
                .build();
    }
}