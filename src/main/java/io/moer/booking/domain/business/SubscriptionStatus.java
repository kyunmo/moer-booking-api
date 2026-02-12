package io.moer.booking.domain.business;

import lombok.Getter;

/**
 * 구독 상태
 */
@Getter
public enum SubscriptionStatus {
    /**
     * 체험판 (30일 무료)
     */
    TRIAL("체험판"),

    /**
     * 활성 (유료 구독 중)
     */
    ACTIVE("활성"),

    /**
     * 만료됨 (결제 실패 또는 체험판 종료)
     */
    EXPIRED("만료됨"),

    /**
     * 취소됨 (사용자가 직접 취소)
     */
    CANCELED("취소됨"),

    /**
     * 정지됨 (관리자가 강제 정지)
     */
    SUSPENDED("정지됨");

    private final String description;

    SubscriptionStatus(String description) {
        this.description = description;
    }

    /**
     * 서비스 사용 가능 여부
     */
    public boolean canUseService() {
        return this == TRIAL || this == ACTIVE;
    }

    /**
     * 결제 필요 여부
     */
    public boolean requiresPayment() {
        return this == EXPIRED;
    }
}
