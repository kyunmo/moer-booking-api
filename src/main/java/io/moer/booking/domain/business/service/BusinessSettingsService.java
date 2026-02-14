package io.moer.booking.domain.business.service;

import io.moer.booking.domain.business.BusinessSettings;
import io.moer.booking.domain.business.repository.BusinessSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * BusinessSettings 서비스
 * - BusinessService에서 self-invocation 문제를 방지하기 위해 별도 Bean으로 분리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessSettingsService {

    private final BusinessSettingsRepository businessSettingsRepository;

    /**
     * 기본 설정 생성 (Settings가 없는 기존 매장용)
     * REQUIRES_NEW: 별도 트랜잭션으로 실행 (readOnly 트랜잭션에서 호출 가능)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BusinessSettings createDefaultSettings(Long businessId) {
        log.info("Creating default settings for business: {}", businessId);

        BusinessSettings settings = BusinessSettings.builder()
                .businessId(businessId)
                .bookingInterval(30)
                .autoConfirm("N")
                .allowOnlineBooking("Y")
                .maxAdvanceBookingDays(30)
                .minAdvanceBookingHours(2)
                .sendConfirmationSms("Y")
                .sendReminderSms("Y")
                .reminderHoursBefore(24)
                .sendCancelSms("Y")
                .kakaoEnabled("N")
                .paymentMethods("CARD,CASH")
                .requireDeposit("N")
                .depositAmount(0)
                .allowCancellation("Y")
                .cancelDeadlineHours(24)
                .noShowPenaltyEnabled("N")
                .timezone("Asia/Seoul")
                .language("ko")
                .build();

        businessSettingsRepository.save(settings);

        log.info("Default settings created for business: {}", businessId);

        return settings;
    }
}
