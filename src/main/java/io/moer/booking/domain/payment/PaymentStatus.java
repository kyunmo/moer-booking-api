package io.moer.booking.domain.payment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 결제 상태
 */
@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    PENDING("결제 대기"),
    COMPLETED("결제 완료"),
    FAILED("결제 실패"),
    REFUNDED("환불 완료");

    private final String description;

    public boolean isCompleted() {
        return this == COMPLETED;
    }

    public boolean canRefund() {
        return this == COMPLETED;
    }

    public boolean isPending() {
        return this == PENDING;
    }
}
