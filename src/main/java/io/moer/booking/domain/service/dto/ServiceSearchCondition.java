package io.moer.booking.domain.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSearchCondition {

    private Long businessId;
    private Long categoryId;  // 카테고리 ID 필터
    private Boolean isActive;  // 활성 여부 필터
    private Long staffId;  // 특정 Staff가 제공 가능한 서비스만 조회
}
