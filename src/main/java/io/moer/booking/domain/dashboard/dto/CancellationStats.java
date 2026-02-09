package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 취소/노쇼 통계
 */
@Getter
@Builder
@AllArgsConstructor
public class CancellationStats {
    /**
     * 취소 건수
     */
    private Integer cancelledCount;

    /**
     * 노쇼 건수
     */
    private Integer noShowCount;

    /**
     * 취소율 (%)
     */
    private Double cancellationRate;

    /**
     * 노쇼율 (%)
     */
    private Double noShowRate;

    /**
     * 취소로 인한 매출 손실액
     */
    private Integer lostRevenue;
}
