package io.moer.booking.domain.payment.dto;

import io.moer.booking.common.util.MaskingUtils;
import io.moer.booking.domain.business.BillingCycle;
import io.moer.booking.domain.business.SubscriptionPlan;
import io.moer.booking.domain.payment.Payment;
import io.moer.booking.domain.payment.PaymentMethod;
import io.moer.booking.domain.payment.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import io.swagger.v3.oas.annotations.media.Schema;

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
    // SECURITY (P1-9): PG 거래번호는 위변조/환불 공격 표적이므로 일반 노출은 마스킹된 형태 사용 권장.
    // 마스킹된 표시용 필드. UI에서 가능하면 이 필드를 사용.
    private String pgTransactionIdMasked;

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

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .businessId(payment.getBusinessId())
                .couponId(payment.getCouponId())
                .subscriptionPlan(payment.getSubscriptionPlan())
                .billingCycle(payment.getBillingCycle())
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
                .pgTransactionIdMasked(MaskingUtils.maskToken(payment.getPgTransactionId()))
                .paidAt(payment.getPaidAt())
                .failedReason(payment.getFailedReason())
                .refundedAt(payment.getRefundedAt())
                .refundAmount(payment.getRefundAmount())
                .cancelReason(payment.getCancelReason())
                .cancelledAt(payment.getCancelledAt())
                .isExtension(payment.getIsExtension())
                .previousBillingEndDate(payment.getPreviousBillingEndDate())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
