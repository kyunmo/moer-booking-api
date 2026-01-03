package io.moer.booking.domain.business.dto;

import io.moer.booking.domain.business.BusinessType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 매장 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessCreateRequest {

    @NotNull(message = "Owner ID는 필수입니다")
    private Long ownerId;

    @NotNull(message = "업종은 필수입니다")
    private BusinessType businessType;

    @NotBlank(message = "매장명은 필수입니다")
    @Size(min = 2, max = 100, message = "매장명은 2~100자 사이여야 합니다")
    private String name;

    private String phone;
    private String address;
    private String description;

    /**
     * 영업시간 (JSONB)
     * 예: {"mon":{"open":"09:00","close":"20:00"}}
     */
    private Map<String, Object> businessHours;

    /**
     * Settings (함께 생성 - 선택)
     */
    private Map<String, Object> settings;
}