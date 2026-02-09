package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 서비스별 통계
 */
@Getter
@Builder
@AllArgsConstructor
public class ServiceStats {
    private Long serviceId;
    private String serviceName;
    private Integer reservationCount;
    private Integer totalRevenue;
    private Integer averagePrice;
    private Double revenuePercentage;
}
