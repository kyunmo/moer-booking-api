package io.moer.booking.domain.payment;

import io.moer.booking.domain.business.BillingCycle;
import io.moer.booking.domain.business.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 결제 엔티티
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    private Long id;
    private Long businessId;
    private Long couponId;

    // 구독 정보
    private SubscriptionPlan subscriptionPlan;
    private BillingCycle billingCycle;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;

    // 금액 정보
    private Integer amount;
    private Integer discountAmount;
    private Integer finalAmount;

    // 쿠폰 정보
    private String couponCode;

    // 결제 정보
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String pgProvider;
    private String pgTransactionId;

    // 웹훅 정보
    private LocalDateTime webhookReceivedAt;
    private Map<String, Object> webhookData; // JSONB

    // 메타데이터
    private LocalDateTime paidAt;
    private String failedReason;
    private LocalDateTime refundedAt;
    private Integer refundAmount;

    // 취소 정보
    private String cancelReason;
    private LocalDateTime cancelledAt;

    // 기간 연장 정보
    private Boolean isExtension;
    private LocalDate previousBillingEndDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 헬퍼 메서드
    public boolean isCompleted() {
        return PaymentStatus.COMPLETED.equals(this.paymentStatus);
    }

    public boolean canRefund() {
        return paymentStatus != null && paymentStatus.canRefund();
    }

    public boolean canCancel() {
        return paymentStatus != null && paymentStatus.canCancel();
    }

    public boolean isPending() {
        return PaymentStatus.PENDING.equals(this.paymentStatus);
    }

    public boolean isFailed() {
        return PaymentStatus.FAILED.equals(this.paymentStatus);
    }

    public boolean isRefunded() {
        return PaymentStatus.REFUNDED.equals(this.paymentStatus);
    }

    public boolean isCancelled() {
        return PaymentStatus.CANCELLED.equals(this.paymentStatus);
    }
}
