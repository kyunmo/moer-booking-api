package io.moer.booking.domain.superadmin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.business.*;
import io.moer.booking.domain.payment.Payment;
import io.moer.booking.domain.payment.PaymentMethod;
import io.moer.booking.domain.payment.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 슈퍼어드민 매장 상세 응답 DTO
 * 구독 상세 정보 + 최근 결제 내역 포함
 */
@Getter
@Builder
@AllArgsConstructor
public class SuperAdminBusinessDetailResponse {

    private Long id;
    private String name;
    private BusinessType businessType;
    private String ownerName;
    private String ownerEmail;
    private String address;
    private String addressDetail;
    private String zipCode;
    private BusinessStatus status;
    private String profileImageUrl;
    private Map<String, Object> businessHours;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // 구독 상세 정보
    private SubscriptionDetail subscription;

    // 최근 결제 내역
    private List<RecentPayment> recentPayments;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SubscriptionDetail {
        private SubscriptionPlan plan;
        private SubscriptionStatus status;
        private BillingCycle billingCycle;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDateTime startDate;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDateTime nextBillingDate;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDateTime trialEndDate;

        private Boolean isTrialActive;
        private Integer currentStaffCount;
        private Integer maxStaff;
        private Integer currentMonthReservationCount;
        private Integer maxMonthlyReservations;

        public static SubscriptionDetail from(Business business) {
            SubscriptionPlan plan = business.getSubscriptionPlan() != null
                    ? business.getSubscriptionPlan() : SubscriptionPlan.FREE;

            return SubscriptionDetail.builder()
                    .plan(plan)
                    .status(business.getSubscriptionStatus())
                    .billingCycle(business.getBillingCycle())
                    .startDate(business.getSubscriptionStartedAt())
                    .nextBillingDate(business.getNextBillingDate())
                    .trialEndDate(business.getTrialEndsAt())
                    .isTrialActive(business.isTrialActive())
                    .currentStaffCount(business.getCurrentStaffCount() != null ? business.getCurrentStaffCount() : 0)
                    .maxStaff(plan.getMaxStaff())
                    .currentMonthReservationCount(business.getCurrentMonthReservationCount() != null ? business.getCurrentMonthReservationCount() : 0)
                    .maxMonthlyReservations(plan.getMaxMonthlyReservations())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RecentPayment {
        private Long id;
        private Integer amount;
        private PaymentStatus status;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        private SubscriptionPlan subscriptionPlan;
        private PaymentMethod paymentMethod;

        public static RecentPayment from(Payment payment) {
            return RecentPayment.builder()
                    .id(payment.getId())
                    .amount(payment.getFinalAmount())
                    .status(payment.getPaymentStatus())
                    .createdAt(payment.getCreatedAt())
                    .subscriptionPlan(payment.getSubscriptionPlan())
                    .paymentMethod(payment.getPaymentMethod())
                    .build();
        }
    }

    public static SuperAdminBusinessDetailResponse from(
            Business business,
            String ownerEmail,
            List<Payment> recentPayments) {

        return SuperAdminBusinessDetailResponse.builder()
                .id(business.getId())
                .name(business.getName())
                .businessType(business.getBusinessType())
                .ownerName(business.getOwnerName())
                .ownerEmail(ownerEmail)
                .address(business.getAddress())
                .addressDetail(business.getAddressDetail())
                .zipCode(business.getZipCode())
                .status(business.getStatus())
                .profileImageUrl(business.getProfileImageUrl())
                .businessHours(business.getBusinessHours())
                .createdAt(business.getCreatedAt())
                .subscription(SubscriptionDetail.from(business))
                .recentPayments(recentPayments.stream()
                        .map(RecentPayment::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
