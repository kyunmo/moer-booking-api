package io.moer.booking.domain.business;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 매장 설정 엔티티
 * DB 테이블: business_settings
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessSettings {

    private Long id;
    private Long businessId;

    // ========================================
    // 예약 설정
    // ========================================

    /**
     * 예약 시간 간격 (분)
     * 기본값: 30
     */
    private Integer bookingInterval;

    /**
     * 예약 자동 확정 (Y/N)
     * 기본값: N
     */
    private String autoConfirm;

    /**
     * 온라인 예약 허용 (Y/N)
     * 기본값: Y
     */
    private String allowOnlineBooking;

    /**
     * 최대 사전 예약 일수
     * 기본값: 30
     */
    private Integer maxAdvanceBookingDays;

    /**
     * 최소 사전 예약 시간 (시간)
     * 기본값: 2
     */
    private Integer minAdvanceBookingHours;

    // ========================================
    // 알림 설정
    // ========================================

    /**
     * 예약 확정 SMS 발송 (Y/N)
     * 기본값: Y
     */
    private String sendConfirmationSms;

    /**
     * 예약 알림 SMS 발송 (Y/N)
     * 기본값: Y
     */
    private String sendReminderSms;

    /**
     * 알림 발송 시간 (예약 시간 N시간 전)
     * 기본값: 24
     */
    private Integer reminderHoursBefore;

    /**
     * 예약 취소 SMS 발송 (Y/N)
     * 기본값: Y
     */
    private String sendCancelSms;

    // ========================================
    // 카카오톡 설정
    // ========================================

    private String kakaoChannelId;
    private String kakaoApiKey;

    /**
     * 카카오톡 알림 사용 (Y/N)
     * 기본값: N
     */
    private String kakaoEnabled;

    // ========================================
    // 결제 설정
    // ========================================

    /**
     * 결제 수단 (콤마 구분)
     * 기본값: CARD,CASH
     */
    private String paymentMethods;

    /**
     * 예약금 필수 (Y/N)
     * 기본값: N
     */
    private String requireDeposit;

    /**
     * 예약금 금액
     * 기본값: 0
     */
    private Integer depositAmount;

    // ========================================
    // 취소 정책
    // ========================================

    /**
     * 예약 취소 허용 (Y/N)
     * 기본값: Y
     */
    private String allowCancellation;

    /**
     * 취소 마감 시간 (예약 시간 N시간 전)
     * 기본값: 24
     */
    private Integer cancelDeadlineHours;

    /**
     * 노쇼 패널티 사용 (Y/N)
     * 기본값: N
     */
    private String noShowPenaltyEnabled;

    // ========================================
    // 기타
    // ========================================

    /**
     * 타임존
     * 기본값: Asia/Seoul
     */
    private String timezone;

    /**
     * 언어
     * 기본값: ko
     */
    private String language;

    // ========================================
    // 고객 등급 설정
    // ========================================

    /**
     * 단골 고객 임계값 (방문 횟수)
     * 기본값: 3
     */
    private Integer regularThreshold;

    /**
     * VIP 고객 임계값 (방문 횟수)
     * 기본값: 10
     */
    private Integer vipThreshold;

    /**
     * VIP 혜택 설명
     */
    private String vipBenefitDescription;

    // ========================================
    // 온보딩
    // ========================================

    private String onboardingCompleted;
    private String onboardingSkipped;
    private String onboardingStepService;
    private String onboardingStepStaff;
    private String onboardingStepReservation;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========================================
    // 헬퍼 메서드 (이름 변경하여 충돌 방지)
    // ========================================

    public boolean hasAutoConfirm() {
        return "Y".equals(this.autoConfirm);
    }

    public boolean allowsOnlineBooking() {
        return "Y".equals(this.allowOnlineBooking);
    }

    public boolean hasKakaoEnabled() {
        return "Y".equals(this.kakaoEnabled);
    }

    public boolean requiresDeposit() {
        return "Y".equals(this.requireDeposit);
    }

    public boolean allowsCancellation() {
        return "Y".equals(this.allowCancellation);
    }

    public boolean hasNoShowPenalty() {
        return "Y".equals(this.noShowPenaltyEnabled);
    }

    public int getRegularThresholdValue() {
        return regularThreshold != null ? regularThreshold : 3;
    }

    public int getVipThresholdValue() {
        return vipThreshold != null ? vipThreshold : 10;
    }
}