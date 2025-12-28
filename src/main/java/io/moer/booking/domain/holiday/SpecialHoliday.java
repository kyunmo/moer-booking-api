package io.moer.booking.domain.holiday;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialHoliday {

    private Long id;
    private Long businessId;

    private LocalDate holidayDate;
    private String title;
    private Boolean isClosed;

    private LocalDateTime createdAt;

    /**
     * 특정 날짜가 휴무일인지 확인
     */
    public boolean isHoliday(LocalDate date) {
        return holidayDate.equals(date) && Boolean.TRUE.equals(isClosed);
    }
}