package io.moer.booking.domain.business;

import lombok.Getter;

/**
 * 구독 플랜 (SaaS 요금제)
 * 2티어: FREE / BASIC
 */
@Getter
public enum SubscriptionPlan {
    /**
     * 무료 플랜
     * - 직원: 1명
     * - 월간 예약: 30건
     * - 서비스: 10개
     * - 가격: 무료
     * - 광고 표시: O
     */
    FREE("무료", 0, 0, 1, 30, 10),

    /**
     * 유료 플랜 (프론트엔드에서는 "PAID"로 표시)
     * - 직원: 5명
     * - 월간 예약: 무제한
     * - 서비스: 무제한
     * - 월간: 18,000원/월 (VAT 포함 19,800원)
     * - 연간: 180,000원/년 (VAT 포함 198,000원, 월 10개월분)
     * - 광고 표시: X
     */
    BASIC("유료", 18000, 180000, 5, -1, -1);

    private final String description;
    private final int monthlyPrice;           // 월 가격 (원)
    private final int yearlyPrice;            // 연 가격 (원)
    private final int maxStaff;               // 최대 직원 수 (-1: 무제한)
    private final int maxMonthlyReservations; // 월간 최대 예약 수 (-1: 무제한)
    private final int maxServices;            // 최대 서비스 수 (-1: 무제한)

    SubscriptionPlan(String description, int monthlyPrice, int yearlyPrice,
                     int maxStaff, int maxMonthlyReservations, int maxServices) {
        this.description = description;
        this.monthlyPrice = monthlyPrice;
        this.yearlyPrice = yearlyPrice;
        this.maxStaff = maxStaff;
        this.maxMonthlyReservations = maxMonthlyReservations;
        this.maxServices = maxServices;
    }

    /**
     * 결제 주기에 따른 가격 반환
     */
    public int getPrice(BillingCycle cycle) {
        if (cycle == null || cycle.isMonthly()) {
            return monthlyPrice;
        }
        return yearlyPrice;
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
     * 서비스 추가 가능 여부
     */
    public boolean canAddService(int currentCount) {
        if (maxServices == -1) return true; // 무제한
        return currentCount < maxServices;
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
