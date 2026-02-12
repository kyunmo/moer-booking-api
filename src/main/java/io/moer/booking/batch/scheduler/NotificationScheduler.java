package io.moer.booking.batch.scheduler;

import io.moer.booking.common.service.EmailService;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.user.User;
import io.moer.booking.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이메일 알림 배치
 * 매일 오전 10시, 11시에 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * 체험판 종료 7일 전 알림
     * Cron: 매일 10:00:00
     */
    @Scheduled(cron = "0 0 10 * * *")
    public void sendTrialExpirationReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysLater = now.plusDays(7);
        log.info("=== 체험판 종료 알림 발송 시작 === (실행 시각: {})", now);

        try {
            // 1. 7일 후 체험판이 종료되는 매장 조회
            List<Business> businesses = businessRepository.findTrialsExpiringInDays(sevenDaysLater, 1);

            if (businesses.isEmpty()) {
                log.info("7일 후 체험판 종료 예정 매장 없음");
                return;
            }

            log.info("알림 발송 대상: {}개 매장", businesses.size());

            // 2. 각 매장 소유자에게 이메일 발송
            int sentCount = 0;
            for (Business business : businesses) {
                try {
                    User owner = userRepository.findById(business.getOwnerId())
                        .orElse(null);

                    if (owner == null) {
                        log.warn("소유자 없음: businessId={}", business.getId());
                        continue;
                    }

                    // 이메일 발송
                    emailService.sendTrialExpirationReminder(
                        owner.getEmail(),
                        owner.getName(),
                        business.getName(),
                        business.getTrialEndsAt()
                    );

                    sentCount++;
                    log.info("체험판 종료 알림 발송: email={}, businessName={}, expiresAt={}",
                        owner.getEmail(), business.getName(), business.getTrialEndsAt());
                } catch (Exception e) {
                    log.error("이메일 발송 실패: businessId={}, error={}",
                        business.getId(), e.getMessage(), e);
                }
            }

            log.info("=== 체험판 종료 알림 발송 완료 === (발송: {}개)", sentCount);
        } catch (Exception e) {
            log.error("체험판 종료 알림 배치 중 오류 발생", e);
        }
    }

    /**
     * 결제 실패 알림
     * Cron: 매일 11:00:00
     */
    @Scheduled(cron = "0 0 11 * * *")
    public void sendPaymentFailureNotifications() {
        LocalDateTime now = LocalDateTime.now();
        log.info("=== 결제 실패 알림 발송 시작 === (실행 시각: {})", now);

        try {
            // 1. 구독 만료된 매장 조회 (최근 1일 이내)
            LocalDateTime oneDayAgo = now.minusDays(1);
            List<Business> expiredBusinesses = businessRepository.findRecentlyExpired(oneDayAgo);

            if (expiredBusinesses.isEmpty()) {
                log.info("최근 만료된 매장 없음");
                return;
            }

            log.info("알림 발송 대상: {}개 매장", expiredBusinesses.size());

            // 2. 각 매장 소유자에게 이메일 발송
            int sentCount = 0;
            for (Business business : expiredBusinesses) {
                try {
                    User owner = userRepository.findById(business.getOwnerId())
                        .orElse(null);

                    if (owner == null) {
                        log.warn("소유자 없음: businessId={}", business.getId());
                        continue;
                    }

                    // 이메일 발송
                    emailService.sendSubscriptionExpiredNotification(
                        owner.getEmail(),
                        owner.getName(),
                        business.getName()
                    );

                    sentCount++;
                    log.info("구독 만료 알림 발송: email={}, businessName={}",
                        owner.getEmail(), business.getName());
                } catch (Exception e) {
                    log.error("이메일 발송 실패: businessId={}, error={}",
                        business.getId(), e.getMessage(), e);
                }
            }

            log.info("=== 결제 실패 알림 발송 완료 === (발송: {}개)", sentCount);
        } catch (Exception e) {
            log.error("결제 실패 알림 배치 중 오류 발생", e);
        }
    }
}
