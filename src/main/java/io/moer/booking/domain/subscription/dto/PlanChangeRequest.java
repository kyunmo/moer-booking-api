package io.moer.booking.domain.subscription.dto;

import io.moer.booking.domain.business.BillingCycle;
import io.moer.booking.domain.business.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "플랜 변경 요청")
public class PlanChangeRequest {

    @NotNull(message = "새 플랜은 필수입니다")
    @Schema(description = "변경할 구독 플랜", example = "BASIC")
    private SubscriptionPlan newPlan;

    @Schema(description = "결제 주기 (FREE→BASIC 변경 시 필수)", example = "MONTHLY")
    private BillingCycle billingCycle;
}
