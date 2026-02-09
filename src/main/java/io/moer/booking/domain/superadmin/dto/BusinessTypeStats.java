package io.moer.booking.domain.superadmin.dto;

import io.moer.booking.domain.business.BusinessType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 업종별 통계
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessTypeStats {
    private BusinessType businessType;
    private Long count;
    private BigDecimal totalRevenue;
}
