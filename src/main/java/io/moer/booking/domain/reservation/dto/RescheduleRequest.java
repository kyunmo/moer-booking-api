package io.moer.booking.domain.reservation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleRequest {

    @NotNull(message = "변경할 날짜는 필수입니다")
    private LocalDate newDate;

    @NotNull(message = "변경할 시작 시간은 필수입니다")
    private LocalTime newStartTime;

    private LocalTime newEndTime;       // null이면 기존 소요시간 유지

    private Long staffId;               // null이면 기존 직원 유지

    private Boolean notifyCustomer;     // 기본값 true

    public boolean shouldNotifyCustomer() {
        return notifyCustomer == null || notifyCustomer;
    }
}
