package io.moer.booking.domain.holiday.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SpecialHolidayCreateRequest {

    @NotNull(message = "휴무일은 필수입니다")
    private LocalDate holidayDate;

    @Size(max = 100, message = "제목은 100자 이내여야 합니다")
    private String title;

    private Boolean isClosed;  // 기본값: true
}