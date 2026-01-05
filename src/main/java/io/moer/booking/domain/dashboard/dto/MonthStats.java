package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MonthStats {
    private Integer totalReservations;
    private Integer totalRevenue;
    private Integer newCustomers;
}