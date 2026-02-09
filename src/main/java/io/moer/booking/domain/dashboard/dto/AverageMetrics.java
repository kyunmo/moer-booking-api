package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 평균 지표
 */
@Getter
@Builder
@AllArgsConstructor
public class AverageMetrics {
    /**
     * 평균 예약 금액
     */
    private Integer averageReservationAmount;

    /**
     * 평균 서비스 시간 (분)
     */
    private Integer averageServiceDuration;

    /**
     * 고객당 평균 방문 횟수
     */
    private Double averageVisitCount;

    /**
     * 고객당 평균 결제액 (LTV)
     */
    private Integer averageCustomerLifetimeValue;

    /**
     * 예약 → 완료 전환율 (%)
     */
    private Double completionRate;
}
