package io.moer.booking.domain.business.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.BusinessStatus;
import io.moer.booking.domain.business.BusinessType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class BusinessResponse {

    private Long id;
    private Long ownerId;
    private BusinessType businessType;
    private String name;
    private String description;
    private String phone;
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

    // 상태
    private BusinessStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Settings (포함 여부는 옵션)
    private Map<String, Object> settings;

    public static BusinessResponse from(Business business) {
        return BusinessResponse.builder()
                .id(business.getId())
                .ownerId(business.getOwnerId())
                .businessType(business.getBusinessType())
                .name(business.getName())
                .description(business.getDescription())
                .phone(business.getPhone())
                .email(business.getEmail())
                .address(business.getAddress())
                .addressDetail(business.getAddressDetail())
                .zipCode(business.getZipCode())
                .latitude(business.getLatitude())
                .longitude(business.getLongitude())
                .openingHours(business.getOpeningHours())
                .regularHolidays(business.getRegularHolidays())
                .logoUrl(business.getLogoUrl())
                .coverImageUrl(business.getCoverImageUrl())
                .images(business.getImages())
                .website(business.getWebsite())
                .instagram(business.getInstagram())
                .facebook(business.getFacebook())
                .status(business.getStatus())
                .createdAt(business.getCreatedAt())
                .updatedAt(business.getUpdatedAt())
                .build();
    }

    public static BusinessResponse from(Business business, Map<String, Object> settings) {
        BusinessResponse response = from(business);
        return BusinessResponse.builder()
                .id(response.getId())
                .ownerId(response.getOwnerId())
                .businessType(response.getBusinessType())
                .name(response.getName())
                .description(response.getDescription())
                .phone(response.getPhone())
                .email(response.getEmail())
                .address(response.getAddress())
                .addressDetail(response.getAddressDetail())
                .zipCode(response.getZipCode())
                .latitude(response.getLatitude())
                .longitude(response.getLongitude())
                .openingHours(response.getOpeningHours())
                .regularHolidays(response.getRegularHolidays())
                .logoUrl(response.getLogoUrl())
                .coverImageUrl(response.getCoverImageUrl())
                .images(response.getImages())
                .website(response.getWebsite())
                .instagram(response.getInstagram())
                .facebook(response.getFacebook())
                .status(response.getStatus())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .settings(settings)
                .build();
    }
}