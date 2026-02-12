package io.moer.booking.domain.coupon;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 쿠폰 엔티티
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {
    private Long id;
    private Long businessId;

    // 쿠폰 정보
    private String code;
    private String name;
    private String description;

    // 할인 정보
    private CouponType couponType;
    private Integer discountAmount;       // 정액 할인 금액
    private Integer discountPercentage;   // 정률 할인 비율 (0~100)
    private Integer maxDiscountAmount;    // 정률 할인 시 최대 할인 금액

    // 사용 조건
    private Integer minOrderAmount;       // 최소 주문 금액
    private Integer maxUsageCount;        // 최대 사용 횟수
    private Integer currentUsageCount;    // 현재 사용 횟수

    // 유효 기간
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    // 상태
    private CouponStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========================================
    // 헬퍼 메서드 - 검증
    // ========================================

    /**
     * 쿠폰 사용 가능 여부 검증
     */
    public void validateUsage(int orderAmount) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 상태 확인
        if (!status.isActive()) {
            throw new BusinessException(ErrorCode.COUPON_NOT_ACTIVE, "사용할 수 없는 쿠폰입니다");
        }

        // 2. 유효 기간 확인
        if (now.isBefore(validFrom)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_ACTIVE,
                "쿠폰 사용 가능 기간이 아닙니다 (시작일: " + validFrom + ")");
        }
        if (now.isAfter(validUntil)) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED,
                "만료된 쿠폰입니다 (만료일: " + validUntil + ")");
        }

        // 3. 최소 주문 금액 확인
        if (minOrderAmount != null && orderAmount < minOrderAmount) {
            throw new BusinessException(ErrorCode.COUPON_MIN_AMOUNT_NOT_MET,
                String.format("최소 주문 금액을 충족하지 못했습니다 (필요: %d원, 현재: %d원)",
                    minOrderAmount, orderAmount));
        }

        // 4. 사용 횟수 확인
        if (maxUsageCount != null && currentUsageCount >= maxUsageCount) {
            throw new BusinessException(ErrorCode.COUPON_LIMIT_EXCEEDED,
                "쿠폰 사용 횟수를 초과했습니다");
        }
    }

    /**
     * 할인 금액 계산
     */
    public int calculateDiscount(int orderAmount) {
        return couponType.calculateDiscount(
            orderAmount,
            discountAmount,
            discountPercentage,
            maxDiscountAmount
        );
    }

    /**
     * 쿠폰이 활성 상태인지 확인
     */
    public boolean isActive() {
        return status != null && status.isActive();
    }

    /**
     * 쿠폰이 만료되었는지 확인
     */
    public boolean isExpired() {
        if (status == CouponStatus.EXPIRED) return true;
        return LocalDateTime.now().isAfter(validUntil);
    }

    /**
     * 사용 가능한 남은 횟수
     */
    public Integer getRemainingUsageCount() {
        if (maxUsageCount == null) return null; // 무제한
        return maxUsageCount - currentUsageCount;
    }
}
