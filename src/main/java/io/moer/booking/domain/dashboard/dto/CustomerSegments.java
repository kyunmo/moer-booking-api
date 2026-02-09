package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 고객 세그먼트 분석
 */
@Getter
@Builder
@AllArgsConstructor
public class CustomerSegments {
    /**
     * VIP 고객 수 (10회 이상)
     */
    private Integer vipCount;

    /**
     * 단골 고객 수 (3~9회)
     */
    private Integer regularCount;

    /**
     * 신규 고객 수 (1회)
     */
    private Integer newCount;

    /**
     * 이탈 고객 수 (3개월 이상 미방문)
     */
    private Integer inactiveCount;

    /**
     * 고객 재방문율 (%)
     */
    private Double returningRate;

    /**
     * 전체 고객 수
     */
    private Integer totalCustomers;
}
