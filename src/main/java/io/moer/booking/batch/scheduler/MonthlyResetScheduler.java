package io.moer.booking.batch.scheduler;

import io.moer.booking.domain.business.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 월간 예약 수 초기화 배치
 * 매월 1일 새벽 3시에 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyResetScheduler {

    private final BusinessRepository businessRepository;

    /**
     * 모든 매장의 월간 예약 수 초기화
     * Cron: 매월 1일 03:00:00
     */
    @Scheduled(cron = "0 0 3 1 * *")
    @Transactional
    public void resetMonthlyReservationCounts() {
        log.info("=== 월간 예약 수 초기화 시작 ===");

        try {
            // 모든 매장의 current_month_reservation_count를 0으로 초기화
            int resetCount = businessRepository.resetMonthlyReservationCounts();

            log.info("=== 월간 예약 수 초기화 완료 === (처리: {}개 매장)", resetCount);
        } catch (Exception e) {
            log.error("월간 예약 수 초기화 중 오류 발생", e);
        }
    }
}
