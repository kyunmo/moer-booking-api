package io.moer.booking.domain.reservation.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleResponse {

    private Long id;
    private String reservationNumber;
    private String customerName;
    private String serviceName;
    private String staffName;
    private LocalDate previousDate;
    private LocalTime previousStartTime;
    private LocalDate newDate;
    private LocalTime newStartTime;
    private LocalTime newEndTime;
    private String status;
    private LocalDateTime updatedAt;
}
