package io.moer.booking.domain.payment.dto;

import io.moer.booking.domain.business.SubscriptionPlan;
import io.moer.booking.domain.payment.Payment;
import io.moer.booking.domain.payment.PaymentMethod;
import io.moer.booking.domain.payment.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 결제 응답 DTO
 */
@Getter
@Builder
public class PaymentResponse {
    private Long id;
    private Long businessId;
    private Long couponId;

    // 구독 정보
    private SubscriptionPlan subscriptionPlan;
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

    // 메타데이터
    private LocalDateTime paidAt;
    private String failedReason;
    private LocalDateTime refundedAt;
    private Integer refundAmount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .businessId(payment.getBusinessId())
                .couponId(payment.getCouponId())
                .subscriptionPlan(payment.getSubscriptionPlan())
                .billingPeriodStart(payment.getBillingPeriodStart())
                .billingPeriodEnd(payment.getBillingPeriodEnd())
                .amount(payment.getAmount())
                .discountAmount(payment.getDiscountAmount())
                .finalAmount(payment.getFinalAmount())
                .couponCode(payment.getCouponCode())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .pgProvider(payment.getPgProvider())
                .pgTransactionId(payment.getPgTransactionId())
                .paidAt(payment.getPaidAt())
                .failedReason(payment.getFailedReason())
                .refundedAt(payment.getRefundedAt())
                .refundAmount(payment.getRefundAmount())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
