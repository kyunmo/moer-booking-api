package io.moer.booking.domain.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 다중 예약 일괄 상태 변경 응답
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkStatusChangeResponse {

    private List<Long> success;
    private List<FailedItem> failed;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedItem {
        private Long reservationId;
        private String reason;
    }
}
