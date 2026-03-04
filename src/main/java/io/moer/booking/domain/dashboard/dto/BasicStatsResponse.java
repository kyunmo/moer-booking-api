package io.moer.booking.domain.dashboard.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BasicStatsResponse {

    private PeriodStats today;
    private PeriodStats thisWeek;
    private int pendingReservations;
    private int unreadReviews;
    private LocalDateTime generatedAt;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PeriodStats {
        private int reservationCount;
        private int completedCount;
        private int cancelledCount;
        private long revenue;
    }
}
