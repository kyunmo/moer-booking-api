package io.moer.booking.domain.business.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 매장 수정 요청 DTO
 * 모든 필드 선택 (null이면 기존 값 유지)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessUpdateRequest {

    @Size(min = 2, max = 100, message = "매장명은 2~100자 사이여야 합니다")
    private String name;

    private String phone;
    private String address;
    private String description;

    /**
     * 영업시간 (JSONB)
     */
    private Map<String, Object> businessHours;
}