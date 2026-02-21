package io.moer.booking.domain.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 고객 등급 임계값 설정 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTierSettingsResponse {

    private Integer regularThreshold;
    private Integer vipThreshold;
    private BigDecimal vipSpendThreshold;
    private String vipBenefitDescription;

    /**
     * 기본값 응답 (설정 미존재 시)
     */
    public static CustomerTierSettingsResponse defaults() {
        return CustomerTierSettingsResponse.builder()
                .regularThreshold(3)
                .vipThreshold(10)
                .vipSpendThreshold(new BigDecimal("500000"))
                .vipBenefitDescription("")
                .build();
    }
}
