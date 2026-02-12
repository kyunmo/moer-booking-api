package io.moer.booking.domain.coupon;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 쿠폰 상태
 */
@Getter
@RequiredArgsConstructor
public enum CouponStatus {
    ACTIVE("활성"),
    EXPIRED("만료"),
    DISABLED("비활성");

    private final String description;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
