package io.moer.booking.domain.business.dto;

import io.moer.booking.domain.business.BusinessType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

    private String description;

    @NotBlank(message = "전화번호는 필수입니다")
    private String phone;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    // 주소
    @NotBlank(message = "주소는 필수입니다")
    private String address;

    private String addressDetail;
    private String zipCode;
    private BigDecimal latitude;
    private BigDecimal longitude;

    // 영업 정보
    private Map<String, Object> openingHours;
    private List<String> regularHolidays;

    // 이미지
    private String logoUrl;
    private String coverImageUrl;
    private List<String> images;

    // 소셜/링크
    private String website;
    private String instagram;
    private String facebook;

    // Settings (함께 생성)
    private Map<String, Object> settings;
}