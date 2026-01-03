package io.moer.booking.domain.holiday;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialHoliday {
    private Long id;
    private Long businessId;
    private LocalDate holidayDate;
    private String reason;
    private LocalDateTime createdAt;
}