package io.moer.booking.batch.scheduler;

import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.SubscriptionStatus;
import io.moer.booking.domain.business.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 체험판 만료 자동 검사 배치
 * 매일 새벽 2시에 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrialExpirationScheduler {

    private final BusinessRepository businessRepository;

    /**
     * 체험판 만료된 매장 자동 처리
     * Cron: 매일 02:00:00
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void checkAndExpireTrials() {
        LocalDateTime now = LocalDateTime.now();
        log.info("=== 체험판 만료 검사 시작 === (실행 시각: {})", now);

        try {
            // 1. 체험판 상태이면서 만료일이 지난 매장 조회
            List<Business> expiredBusinesses = businessRepository.findExpiredTrials(now);

            if (expiredBusinesses.isEmpty()) {
                log.info("만료된 체험판 없음");
                return;
            }

            log.info("만료된 체험판 발견: {}개", expiredBusinesses.size());

            // 2. 각 매장의 상태를 EXPIRED로 변경
            int expiredCount = 0;
            for (Business business : expiredBusinesses) {
                try {
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
                        .billingCycle(business.getBillingCycle())
                        .subscriptionStatus(SubscriptionStatus.EXPIRED) // TRIAL → EXPIRED
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

                    businessRepository.update(updatedBusiness);
                    expiredCount++;

                    log.info("체험판 만료 처리: businessId={}, name={}, trialEndsAt={}",
                        business.getId(), business.getName(), business.getTrialEndsAt());
                } catch (Exception e) {
                    log.error("체험판 만료 처리 실패: businessId={}, error={}",
                        business.getId(), e.getMessage(), e);
                }
            }

            log.info("=== 체험판 만료 검사 완료 === (처리: {}/{}개)", expiredCount, expiredBusinesses.size());
        } catch (Exception e) {
            log.error("체험판 만료 검사 중 오류 발생", e);
        }
    }
}
