package io.moer.booking.domain.business;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 매장 엔티티
 * DB 테이블: businesses
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business {
    private Long id;
    private Long ownerId;
    private String name;

    /**
     * 업종 (Enum)
     * DB: VARCHAR(50)
     */
    private BusinessType businessType;

    private String phone;
    private String address;
    private String description;

    /**
     * 영업시간 (JSONB)
     * DB: JSONB
     * 예: {"mon":{"open":"09:00","close":"20:00"}, "tue":...}
     */
    private Map<String, Object> businessHours;

    /**
     * 상태 (Enum)
     * DB: VARCHAR(20)
     */
    private BusinessStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========================================
    // 헬퍼 메서드 - 상태 확인
    // ========================================

    public boolean isActive() {
        return BusinessStatus.ACTIVE.equals(this.status);
    }

    public boolean isInactive() {
        return BusinessStatus.INACTIVE.equals(this.status);
    }

    public boolean isSuspended() {
        return BusinessStatus.SUSPENDED.equals(this.status);
    }

    // ========================================
    // 헬퍼 메서드 - 업종 확인
    // ========================================

    public boolean isBeautyShop() {
        return BusinessType.BEAUTY_SHOP.equals(this.businessType);
    }

    public boolean isPilates() {
        return BusinessType.PILATES.equals(this.businessType);
    }

    public boolean isCafe() {
        return BusinessType.CAFE.equals(this.businessType);
    }
}