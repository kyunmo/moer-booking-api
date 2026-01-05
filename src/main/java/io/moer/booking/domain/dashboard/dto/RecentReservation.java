package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RecentReservation {
    private Long id;
    private String reservationNumber;
    private String customerName;
    private String reservationDate;
    private String startTime;
    private String endTime;
    private String status;
}