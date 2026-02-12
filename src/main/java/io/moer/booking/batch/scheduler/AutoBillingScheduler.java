package io.moer.booking.batch.scheduler;

import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.SubscriptionPlan;
import io.moer.booking.domain.business.SubscriptionStatus;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.payment.Payment;
import io.moer.booking.domain.payment.PaymentMethod;
import io.moer.booking.domain.payment.PaymentStatus;
import io.moer.booking.domain.payment.repository.PaymentRepository;
import io.moer.booking.domain.payment.service.FakePGService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 자동 결제 배치
 * 매일 새벽 4시에 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoBillingScheduler {

    private final BusinessRepository businessRepository;
    private final PaymentRepository paymentRepository;
    private final FakePGService fakePGService;

    /**
     * 자동 결제 처리
     * Cron: 매일 04:00:00
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void processAutoBilling() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = now.toLocalDate().atStartOfDay();
        log.info("=== 자동 결제 처리 시작 === (실행 시각: {})", now);

        try {
            // 1. 오늘 결제일이 도래한 매장 조회
            List<Business> businesses = businessRepository.findBusinessesForAutoBilling(today);

            if (businesses.isEmpty()) {
                log.info("오늘 자동 결제할 매장 없음");
                return;
            }

            log.info("자동 결제 대상 매장: {}개", businesses.size());

            // 2. 각 매장별 자동 결제 시도
            int successCount = 0;
            int failCount = 0;

            for (Business business : businesses) {
                try {
                    // FREE 플랜은 자동 결제 제외
                    if (business.getSubscriptionPlan() == SubscriptionPlan.FREE) {
                        log.info("FREE 플랜 제외: businessId={}", business.getId());
                        continue;
                    }

                    // 결제 처리
                    boolean success = processPayment(business);
                    if (success) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    log.error("자동 결제 처리 실패: businessId={}, error={}",
                        business.getId(), e.getMessage(), e);
                    failCount++;
                }
            }

            log.info("=== 자동 결제 처리 완료 === (성공: {}개, 실패: {}개)", successCount, failCount);
        } catch (Exception e) {
            log.error("자동 결제 배치 중 오류 발생", e);
        }
    }

    /**
     * 개별 매장 결제 처리
     */
    private boolean processPayment(Business business) {
        try {
            // 1. Payment 생성
            int amount = business.getSubscriptionPlan().getMonthlyPrice();
            LocalDateTime now = LocalDateTime.now();
            LocalDate billingStart = now.toLocalDate();
            LocalDate billingEnd = billingStart.plusMonths(1);

            Payment payment = Payment.builder()
                .businessId(business.getId())
                .subscriptionPlan(business.getSubscriptionPlan())
                .amount(amount)
                .discountAmount(0)
                .finalAmount(amount)
                .paymentMethod(PaymentMethod.CARD) // 자동 결제는 카드만
                .paymentStatus(PaymentStatus.PENDING)
                .billingPeriodStart(billingStart)
                .billingPeriodEnd(billingEnd)
                .build();

            paymentRepository.save(payment);

            // 2. PG 호출 (Fake)
            Map<String, Object> pgResponse = fakePGService.requestPayment(
                amount,
                PaymentMethod.CARD.name()
            );

            String pgStatus = (String) pgResponse.get("status");
            PaymentStatus newStatus = "COMPLETED".equals(pgStatus)
                ? PaymentStatus.COMPLETED
                : PaymentStatus.FAILED;

            // 3. Payment 업데이트
            Payment updatedPayment = Payment.builder()
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
                .paymentStatus(newStatus)
                .pgProvider((String) pgResponse.get("provider"))
                .pgTransactionId((String) pgResponse.get("transactionId"))
                .webhookData(pgResponse)
                .paidAt(newStatus == PaymentStatus.COMPLETED ? now : null)
                .failedReason(newStatus == PaymentStatus.FAILED ? (String) pgResponse.get("failReason") : null)
                .build();

            paymentRepository.update(updatedPayment);

            // 4. 결제 성공 시 다음 결제일 업데이트
            if (newStatus == PaymentStatus.COMPLETED) {
                Business updatedBusiness = Business.builder()
                    .id(business.getId())
                    .ownerId(business.getOwnerId())
                    .name(business.getName())
                    .businessType(business.getBusinessType())
                    .phone(business.getPhone())
                    .address(business.getAddress())
                    .description(business.getDescription())
                    .businessHours(business.getBusinessHours())
                    .status(business.getStatus())
                    .subscriptionPlan(business.getSubscriptionPlan())
                    .subscriptionStatus(business.getSubscriptionStatus())
                    .trialStartedAt(business.getTrialStartedAt())
                    .trialEndsAt(business.getTrialEndsAt())
                    .subscriptionStartedAt(business.getSubscriptionStartedAt())
                    .nextBillingDate(now.plusMonths(1)) // 다음 달로 연장
                    .currentStaffCount(business.getCurrentStaffCount())
                    .currentMonthReservationCount(business.getCurrentMonthReservationCount())
                    .dailyRevenueGoal(business.getDailyRevenueGoal())
                    .monthlyRevenueGoal(business.getMonthlyRevenueGoal())
                    .monthlyNewCustomerGoal(business.getMonthlyNewCustomerGoal())
                    .build();

                businessRepository.update(updatedBusiness);

                log.info("자동 결제 성공: businessId={}, amount={}원, nextBillingDate={}",
                    business.getId(), amount, updatedBusiness.getNextBillingDate());
                return true;
            } else {
                // 5. 결제 실패 시 구독 상태를 EXPIRED로 변경
                Business expiredBusiness = Business.builder()
                    .id(business.getId())
                    .ownerId(business.getOwnerId())
                    .name(business.getName())
                    .businessType(business.getBusinessType())
                    .phone(business.getPhone())
                    .address(business.getAddress())
                    .description(business.getDescription())
                    .businessHours(business.getBusinessHours())
                    .status(business.getStatus())
                    .subscriptionPlan(business.getSubscriptionPlan())
                    .subscriptionStatus(SubscriptionStatus.EXPIRED) // ACTIVE → EXPIRED
                    .trialStartedAt(business.getTrialStartedAt())
                    .trialEndsAt(business.getTrialEndsAt())
                    .subscriptionStartedAt(business.getSubscriptionStartedAt())
                    .nextBillingDate(business.getNextBillingDate())
                    .currentStaffCount(business.getCurrentStaffCount())
                    .currentMonthReservationCount(business.getCurrentMonthReservationCount())
                    .dailyRevenueGoal(business.getDailyRevenueGoal())
                    .monthlyRevenueGoal(business.getMonthlyRevenueGoal())
                    .monthlyNewCustomerGoal(business.getMonthlyNewCustomerGoal())
                    .build();

                businessRepository.update(expiredBusiness);

                log.warn("자동 결제 실패: businessId={}, reason={}",
                    business.getId(), updatedPayment.getFailedReason());
                return false;
            }
        } catch (Exception e) {
            log.error("결제 처리 중 오류: businessId={}, error={}",
                business.getId(), e.getMessage(), e);
            return false;
        }
    }
}
