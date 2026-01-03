package io.moer.booking.domain.reservation.dto;

import io.moer.booking.domain.reservation.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 예약 검색 조건 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationSearchCondition {

    private Long businessId;
    private Long customerId;
    private Long staffId;
    private ReservationStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
}