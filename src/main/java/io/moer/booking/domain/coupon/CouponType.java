package io.moer.booking.domain.coupon;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 쿠폰 타입
 */
@Getter
@RequiredArgsConstructor
public enum CouponType {
    PERCENTAGE("정률 할인"),      // 10%, 20% 등
    FIXED_AMOUNT("정액 할인");    // 5,000원, 10,000원 등

    private final String description;

    /**
     * 할인 금액 계산
     */
    public int calculateDiscount(int orderAmount, Integer discountAmount, Integer discountPercentage, Integer maxDiscountAmount) {
        if (this == PERCENTAGE) {
            // 정률 할인
            if (discountPercentage == null || discountPercentage <= 0 || discountPercentage > 100) {
                return 0;
            }
            int discount = (int) (orderAmount * discountPercentage / 100.0);

            // 최대 할인 금액 제한
            if (maxDiscountAmount != null && discount > maxDiscountAmount) {
                return maxDiscountAmount;
            }
            return discount;
        } else {
            // 정액 할인
            if (discountAmount == null || discountAmount <= 0) {
                return 0;
            }
            // 주문 금액보다 큰 할인은 불가
            return Math.min(discountAmount, orderAmount);
        }
    }
}
