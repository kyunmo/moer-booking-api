package io.moer.booking.domain.business;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
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
    private String ownerName;  // JOIN 조회 시 사장님 이름
    private String name;

    /**
     * 매장 슬러그 (고객용 URL: /booking/{slug})
     */
    private String slug;

    /**
     * 업종 (Enum)
     * DB: VARCHAR(50)
     */
    private BusinessType businessType;

    private String phone;
    private String address;

    /**
     * 상세주소 (예: 2층 201호)
     */
    private String addressDetail;

    /**
     * 우편번호 (예: 06234)
     */
    private String zipCode;

    private String description;

    /**
     * 매장 프로필 이미지 URL
     */
    private String profileImageUrl;

    /**
     * 매장 갤러리 이미지 URL 목록 (JSONB)
     */
    private List<String> galleryImages;

    /**
     * 위도 (위치 기반 검색용)
     */
    private Double latitude;

    /**
     * 경도 (위치 기반 검색용)
     */
    private Double longitude;

    /**
     * 태그 (콤마 구분: "예약가능,주차가능,카드결제")
     */
    private String tags;

    /**
     * 평균 평점 (리뷰 비정규화)
     */
    private Double averageRating;

    /**
     * 리뷰 수 (비정규화)
     */
    private Integer reviewCount;

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

    /**
     * 일일 매출 목표
     */
    private Integer dailyRevenueGoal;

    /**
     * 월간 매출 목표
     */
    private Integer monthlyRevenueGoal;

    /**
     * 월간 신규 고객 목표
     */
    private Integer monthlyNewCustomerGoal;

    /**
     * 구독 플랜 (SaaS 요금제)
     */
    private SubscriptionPlan subscriptionPlan;

    /**
     * 결제 주기 (MONTHLY / YEARLY)
     */
    private BillingCycle billingCycle;

    /**
     * 구독 상태
     */
    private SubscriptionStatus subscriptionStatus;

    /**
     * 무료 체험 시작일
     */
    private LocalDateTime trialStartedAt;

    /**
     * 무료 체험 종료일 (30일)
     */
    private LocalDateTime trialEndsAt;

    /**
     * 유료 구독 시작일
     */
    private LocalDateTime subscriptionStartedAt;

    /**
     * 다음 결제 예정일
     */
    private LocalDateTime nextBillingDate;

    /**
     * 현재 활성 직원 수 (플랜 제한 체크용)
     */
    private Integer currentStaffCount;

    /**
     * 이번 달 예약 수 (플랜 제한 체크용)
     */
    private Integer currentMonthReservationCount;

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

    // ========================================
    // 헬퍼 메서드 - 구독 관리
    // ========================================

    /**
     * 체험판 활성 여부
     */
    public boolean isTrialActive() {
        if (subscriptionStatus != SubscriptionStatus.TRIAL) return false;
        if (trialEndsAt == null) return false;
        return LocalDateTime.now().isBefore(trialEndsAt);
    }

    /**
     * 체험판 만료 여부
     */
    public boolean isTrialExpired() {
        if (subscriptionStatus != SubscriptionStatus.TRIAL) return false;
        if (trialEndsAt == null) return true;
        return LocalDateTime.now().isAfter(trialEndsAt);
    }

    /**
     * 체험판 남은 일수
     */
    public long getDaysUntilTrialEnd() {
        if (trialEndsAt == null) return 0;
        return java.time.Duration.between(LocalDateTime.now(), trialEndsAt).toDays();
    }

    /**
     * 서비스 사용 가능 여부
     */
    public boolean canUseService() {
        if (subscriptionStatus == null) return false;
        return subscriptionStatus.canUseService();
    }

    /**
     * 직원 추가 가능 여부 (플랜 제한 체크)
     */
    public boolean canAddStaff() {
        if (subscriptionPlan == null) return true;
        if (currentStaffCount == null) return true;
        return subscriptionPlan.canAddStaff(currentStaffCount);
    }

    /**
     * 예약 생성 가능 여부 (플랜 제한 체크)
     */
    public boolean canCreateReservation() {
        if (subscriptionPlan == null) return true;
        if (currentMonthReservationCount == null) return true;
        return subscriptionPlan.canCreateReservation(currentMonthReservationCount);
    }

    /**
     * 무료 플랜 여부
     */
    public boolean isFreePlan() {
        return subscriptionPlan == SubscriptionPlan.FREE;
    }

    /**
     * 유료 플랜 여부
     */
    public boolean isPaidPlan() {
        return subscriptionPlan != null && subscriptionPlan.isPaid();
    }
}