package io.moer.booking.domain.business.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.BusinessSettings;
import io.moer.booking.domain.business.BusinessStatus;
import io.moer.booking.domain.business.BusinessType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 매장 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class BusinessResponse {

    private Long id;
    private Long ownerId;
    private String ownerName;
    private BusinessType businessType;
    private String name;
    private String slug;
    private String phone;
    private String address;
    private String addressDetail;
    private String zipCode;
    private String description;
    private Map<String, Object> businessHours;
    private BusinessStatus status;
    private String profileImageUrl;
    private Integer dailyRevenueGoal;
    private Integer monthlyRevenueGoal;
    private Integer monthlyNewCustomerGoal;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Settings (선택적 포함)
    private BusinessSettings settings;

    /**
     * Entity → DTO 변환
     */
    public static BusinessResponse from(Business business) {
        return BusinessResponse.builder()
                .id(business.getId())
                .ownerId(business.getOwnerId())
                .ownerName(business.getOwnerName())
                .businessType(business.getBusinessType())
                .name(business.getName())
                .slug(business.getSlug())
                .phone(business.getPhone())
                .address(business.getAddress())
                .addressDetail(business.getAddressDetail())
                .zipCode(business.getZipCode())
                .description(business.getDescription())
                .businessHours(business.getBusinessHours())
                .status(business.getStatus())
                .profileImageUrl(business.getProfileImageUrl())
                .dailyRevenueGoal(business.getDailyRevenueGoal())
                .monthlyRevenueGoal(business.getMonthlyRevenueGoal())
                .monthlyNewCustomerGoal(business.getMonthlyNewCustomerGoal())
                .createdAt(business.getCreatedAt())
                .updatedAt(business.getUpdatedAt())
                .build();
    }

    /**
     * Entity → DTO 변환 (Settings 포함)
     */
    public static BusinessResponse from(Business business, BusinessSettings settings) {
        return BusinessResponse.builder()
                .id(business.getId())
                .ownerId(business.getOwnerId())
                .ownerName(business.getOwnerName())
                .businessType(business.getBusinessType())
                .name(business.getName())
                .slug(business.getSlug())
                .phone(business.getPhone())
                .address(business.getAddress())
                .addressDetail(business.getAddressDetail())
                .zipCode(business.getZipCode())
                .description(business.getDescription())
                .businessHours(business.getBusinessHours())
                .status(business.getStatus())
                .profileImageUrl(business.getProfileImageUrl())
                .dailyRevenueGoal(business.getDailyRevenueGoal())
                .monthlyRevenueGoal(business.getMonthlyRevenueGoal())
                .monthlyNewCustomerGoal(business.getMonthlyNewCustomerGoal())
                .createdAt(business.getCreatedAt())
                .updatedAt(business.getUpdatedAt())
                .settings(settings)
                .build();
    }
}