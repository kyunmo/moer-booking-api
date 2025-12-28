package io.moer.booking.domain.business;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Business {

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

    // 영업 정보 (JSONB → Map)
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

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 비즈니스 로직
    public void activate() {
        this.status = BusinessStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = BusinessStatus.INACTIVE;
    }

    public void suspend() {
        this.status = BusinessStatus.SUSPENDED;
    }
}