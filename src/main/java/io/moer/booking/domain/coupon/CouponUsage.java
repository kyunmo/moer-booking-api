package io.moer.booking.domain.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 쿠폰 사용 내역
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponUsage {
    private Long id;
    private Long couponId;
    private Long userId;
    private Long paymentId;

    // 사용 정보
    private Integer discountAmount;  // 실제 할인된 금액
    private LocalDateTime usedAt;

    // 취소 정보
    private String canceled;         // Y/N
    private LocalDateTime canceledAt;

    // 헬퍼 메서드
    public boolean isCanceled() {
        return "Y".equals(canceled);
    }
}
