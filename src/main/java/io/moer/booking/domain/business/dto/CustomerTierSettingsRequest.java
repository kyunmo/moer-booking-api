package io.moer.booking.domain.business.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객 등급 임계값 설정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTierSettingsRequest {

    @Min(value = 1, message = "단골 임계값은 1 이상이어야 합니다")
    private Integer regularThreshold;

    @Min(value = 1, message = "VIP 임계값은 1 이상이어야 합니다")
    private Integer vipThreshold;

    private String vipBenefitDescription;
}
