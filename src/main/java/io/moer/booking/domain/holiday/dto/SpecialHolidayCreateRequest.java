package io.moer.booking.domain.holiday.dto;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "휴무일 이름은 필수입니다")
    private String name;

    @NotNull(message = "휴무 날짜는 필수입니다")
    private LocalDate date;

    @NotBlank(message = "휴무일 유형은 필수입니다")
    private String type;  // REGULAR, TEMPORARY, NATIONAL

    private String reason;  // 상세 사유 (선택)
}