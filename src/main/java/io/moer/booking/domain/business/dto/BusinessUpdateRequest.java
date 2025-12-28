package io.moer.booking.domain.business.dto;

import jakarta.validation.constraints.Email;
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
public class BusinessUpdateRequest {

    @Size(min = 2, max = 100, message = "매장명은 2~100자 사이여야 합니다")
    private String name;

    private String description;
    private String phone;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    // 주소
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
}