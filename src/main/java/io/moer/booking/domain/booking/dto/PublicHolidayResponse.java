package io.moer.booking.domain.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.holiday.SpecialHoliday;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@Schema(description = "Public 휴무일 응답")
public class PublicHolidayResponse {

    @Schema(description = "휴무일 ID", example = "1")
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "휴무 날짜", example = "2026-03-01")
    private LocalDate date;

    @Schema(description = "휴무 사유", example = "삼일절")
    private String reason;

    public static PublicHolidayResponse from(SpecialHoliday holiday) {
        return PublicHolidayResponse.builder()
                .id(holiday.getId())
                .date(holiday.getDate())
                .reason(holiday.getReason() != null ? holiday.getReason() : holiday.getName())
                .build();
    }
}
