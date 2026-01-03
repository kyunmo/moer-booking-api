package io.moer.booking.domain.holiday.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialHolidayCreateRequest {

    @NotNull(message = "휴무 날짜는 필수입니다")
    private LocalDate holidayDate;

    private String reason;  // 휴무 사유 (예: 설날, 추석, 개인 사정)
}