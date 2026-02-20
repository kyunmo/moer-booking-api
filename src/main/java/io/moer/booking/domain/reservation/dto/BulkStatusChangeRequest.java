package io.moer.booking.domain.reservation.dto;

import io.moer.booking.domain.reservation.ReservationStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 다중 예약 일괄 상태 변경 요청
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BulkStatusChangeRequest {

    @NotEmpty(message = "예약 ID 목록은 필수입니다")
    private List<Long> reservationIds;

    @NotNull(message = "변경할 상태는 필수입니다")
    private ReservationStatus status;
}
