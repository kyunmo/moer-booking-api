package io.moer.booking.domain.superadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 매장별 매출 랭킹
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessRevenueRank {
    private Long businessId;
    private String businessName;
    private String ownerName;
    private BigDecimal totalRevenue;
    private Long reservationCount;
    private Integer rank;
}
