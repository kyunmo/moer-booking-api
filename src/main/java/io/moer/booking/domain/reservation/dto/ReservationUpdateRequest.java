package io.moer.booking.domain.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationUpdateRequest {

    private Long staffId;
    private List<Long> serviceIds;
    private LocalDate reservationDate;
    private LocalTime startTime;
    private String customerRequest;
    private String adminMemo;
}