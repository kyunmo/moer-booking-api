package io.moer.booking.domain.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

/**
 * 예약 가용성 확인 응답
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {

    private boolean available;
    private List<ConflictInfo> conflicts;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictInfo {
        private Long reservationId;
        private String customerName;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;

        private String serviceName;
    }
}
