package io.moer.booking.domain.payment.dto;

import io.moer.booking.domain.business.BillingCycle;
import io.moer.booking.domain.business.SubscriptionPlan;
import io.moer.booking.domain.payment.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "결제 생성 요청")
public class PaymentCreateRequest {

    @NotNull(message = "구독 플랜은 필수입니다")
    @Schema(description = "구독 플랜", example = "BASIC")
    private SubscriptionPlan plan;

    @NotNull(message = "결제 주기는 필수입니다")
    @Schema(description = "결제 주기", example = "MONTHLY")
    private BillingCycle billingCycle;

    @NotNull(message = "결제 수단은 필수입니다")
    @Schema(description = "결제 수단", example = "CARD")
    private PaymentMethod paymentMethod;

    @Schema(description = "쿠폰 코드", example = "WELCOME2026")
    private String couponCode;
}
