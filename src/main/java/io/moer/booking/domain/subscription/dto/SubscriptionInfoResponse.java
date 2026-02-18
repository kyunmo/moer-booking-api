package io.moer.booking.domain.subscription.dto;

import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.SubscriptionPlan;
import io.moer.booking.domain.business.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class SubscriptionInfoResponse {

    // 구독 정보
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private String planDescription;
    private Integer monthlyPrice;

    // 체험판 정보
    private Boolean isTrialActive;
    private LocalDateTime trialStartedAt;
    private LocalDateTime trialEndsAt;
    private Long daysUntilTrialEnd;

    // 유료 구독 정보
    private LocalDateTime subscriptionStartedAt;
    private LocalDateTime nextBillingDate;

    // 플랜 제한
    private Integer maxStaff;
    private Integer maxMonthlyReservations;

    // 현재 사용량
    private Integer currentStaffCount;
    private Integer currentMonthReservationCount;

    // 사용 가능 여부
    private Boolean canUseService;
    private Boolean canAddStaff;
    private Boolean canCreateReservation;

    public static SubscriptionInfoResponse from(Business business) {
        SubscriptionPlan plan = business.getSubscriptionPlan() != null
                ? business.getSubscriptionPlan()
                : SubscriptionPlan.FREE;

        return SubscriptionInfoResponse.builder()
                .plan(plan)
                .status(business.getSubscriptionStatus())
                .planDescription(plan.getDescription())
                .monthlyPrice(plan.getMonthlyPrice())
                .isTrialActive(business.isTrialActive())
                .trialStartedAt(business.getTrialStartedAt())
                .trialEndsAt(business.getTrialEndsAt())
                .daysUntilTrialEnd(business.getDaysUntilTrialEnd())
                .subscriptionStartedAt(business.getSubscriptionStartedAt())
                .nextBillingDate(business.getNextBillingDate())
                .maxStaff(plan.getMaxStaff())
                .maxMonthlyReservations(plan.getMaxMonthlyReservations())
                .currentStaffCount(business.getCurrentStaffCount())
                .currentMonthReservationCount(business.getCurrentMonthReservationCount())
                .canUseService(business.canUseService())
                .canAddStaff(business.canAddStaff())
                .canCreateReservation(business.canCreateReservation())
                .build();
    }
}
