package io.moer.booking.domain.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 사용자 엔티티
 * DB 테이블: users
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private Long id;
    private String email;
    private String password;
    private String name;
    private String phone;

    /**
     * 역할 (Enum)
     * DB: VARCHAR(20)
     */
    private UserRole role;

    /**
     * 상태 (Enum)
     * DB: VARCHAR(20)
     */
    private UserStatus status;

    private Long staffId;
    private Long businessId;

    /**
     * 이메일 인증 여부 (Y/N)
     * DB: CHAR(1)
     */
    private String emailVerified;

    /**
     * 체험판 관련 필드
     */
    private LocalDateTime trialStartedAt;
    private LocalDateTime trialExpiresAt;
    private String isPremium;  // Y/N

    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void updateBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    // ========================================
    // 헬퍼 메서드 - 역할 확인
    // ========================================

    public boolean isSuperAdmin() {
        return this.role == UserRole.SUPER_ADMIN;
    }

    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }

    public boolean isOwner() {
        return this.role == UserRole.OWNER;
    }

    public boolean isStaff() {
        return this.role == UserRole.STAFF;
    }

    // ========================================
    // 헬퍼 메서드 - 상태 확인
    // ========================================

    public boolean checkEmailVerified() {
        return "Y".equals(this.emailVerified);
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    // ========================================
    // 헬퍼 메서드 - 권한 확인
    // ========================================

    /**
     * 특정 Business에 접근 가능한지 확인
     */
    public boolean canAccessBusiness(Long businessId) {
        if (isSuperAdmin() || isAdmin()) {
            return true;  // SUPER_ADMIN, ADMIN 모두 전체 접근
        }
        return this.businessId != null && this.businessId.equals(businessId);
    }

    /**
     * 특정 Staff에 접근 가능한지 확인
     */
    public boolean canAccessStaff(Long staffId) {
        if (isSuperAdmin() || isAdmin()) {
            return true;
        }
        if (isOwner()) {
            return true;
        }
        if (isStaff()) {
            return this.staffId != null && this.staffId.equals(staffId);
        }
        return false;
    }

    // ========================================
    // 헬퍼 메서드 - 체험판 관련
    // ========================================

    /**
     * 체험판 진행 중인지 확인
     */
    public boolean isInTrial() {
        return "N".equals(isPremium) &&
               trialExpiresAt != null &&
               LocalDateTime.now().isBefore(trialExpiresAt);
    }

    /**
     * 체험판이 만료되었는지 확인
     */
    public boolean isTrialExpired() {
        return "N".equals(isPremium) &&
               trialExpiresAt != null &&
               LocalDateTime.now().isAfter(trialExpiresAt);
    }

    /**
     * 남은 체험판 일수 계산
     */
    public int getRemainingTrialDays() {
        if (isTrialExpired() || isPremiumUser()) {
            return 0;
        }
        if (trialExpiresAt == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(LocalDateTime.now(), trialExpiresAt);
        return (int) Math.max(0, days);
    }

    /**
     * 프리미엄 사용자인지 확인
     */
    public boolean isPremiumUser() {
        return "Y".equals(isPremium);
    }
}