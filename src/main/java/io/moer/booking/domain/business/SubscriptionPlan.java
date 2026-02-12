package io.moer.booking.domain.business;

import lombok.Getter;

/**
 * 구독 플랜 (SaaS 요금제)
 */
@Getter
public enum SubscriptionPlan {
    /**
     * 무료 플랜
     * - 직원: 1명
     * - 월간 예약: 30건
     * - 가격: 무료
     */
    FREE("무료", 0, 1, 30),

    /**
     * 베이직 플랜
     * - 직원: 3명
     * - 월간 예약: 100건
     * - 가격: 29,000원/월
     */
    BASIC("베이직", 29000, 3, 100),

    /**
     * 프로 플랜
     * - 직원: 10명
     * - 월간 예약: 500건
     * - 가격: 79,000원/월
     */
    PRO("프로", 79000, 10, 500),

    /**
     * 엔터프라이즈 플랜
     * - 직원: 무제한
     * - 월간 예약: 무제한
     * - 가격: 문의
     */
    ENTERPRISE("엔터프라이즈", 0, -1, -1);

    private final String description;
    private final int monthlyPrice;        // 월 가격 (원)
    private final int maxStaff;            // 최대 직원 수 (-1: 무제한)
    private final int maxMonthlyReservations; // 월간 최대 예약 수 (-1: 무제한)

    SubscriptionPlan(String description, int monthlyPrice, int maxStaff, int maxMonthlyReservations) {
        this.description = description;
        this.monthlyPrice = monthlyPrice;
        this.maxStaff = maxStaff;
        this.maxMonthlyReservations = maxMonthlyReservations;
    }

    /**
     * 직원 추가 가능 여부
     */
    public boolean canAddStaff(int currentStaffCount) {
        if (maxStaff == -1) return true; // 무제한
        return currentStaffCount < maxStaff;
    }

    /**
     * 예약 생성 가능 여부
     */
    public boolean canCreateReservation(int currentMonthReservationCount) {
        if (maxMonthlyReservations == -1) return true; // 무제한
        return currentMonthReservationCount < maxMonthlyReservations;
    }

    /**
     * 무료 플랜인지 확인
     */
    public boolean isFree() {
        return this == FREE;
    }

    /**
     * 유료 플랜인지 확인
     */
    public boolean isPaid() {
        return this != FREE;
    }
}
