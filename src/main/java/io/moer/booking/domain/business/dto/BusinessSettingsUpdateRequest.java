package io.moer.booking.domain.business.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매장 설정 수정 요청 DTO
 * 모든 필드 선택 (null이면 기존 값 유지)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessSettingsUpdateRequest {

    // ========================================
    // 예약 설정
    // ========================================

    /**
     * 예약 시간 간격 (분)
     */
    private Integer bookingInterval;

    /**
     * 예약 자동 확정 (Y/N)
     */
    private String autoConfirm;

    /**
     * 온라인 예약 허용 (Y/N)
     */
    private String allowOnlineBooking;

    /**
     * 최대 사전 예약 일수
     */
    private Integer maxAdvanceBookingDays;

    /**
     * 최소 사전 예약 시간 (시간)
     */
    private Integer minAdvanceBookingHours;

    // ========================================
    // 알림 설정
    // ========================================

    /**
     * 예약 확정 SMS 발송 (Y/N)
     */
    private String sendConfirmationSms;

    /**
     * 예약 알림 SMS 발송 (Y/N)
     */
    private String sendReminderSms;

    /**
     * 알림 발송 시간 (예약 시간 N시간 전)
     */
    private Integer reminderHoursBefore;

    /**
     * 예약 취소 SMS 발송 (Y/N)
     */
    private String sendCancelSms;

    // ========================================
    // 카카오톡 설정
    // ========================================

    private String kakaoChannelId;
    private String kakaoApiKey;

    /**
     * 카카오톡 알림 사용 (Y/N)
     */
    private String kakaoEnabled;

    // ========================================
    // 결제 설정
    // ========================================

    /**
     * 결제 수단 (콤마 구분)
     */
    private String paymentMethods;

    /**
     * 예약금 필수 (Y/N)
     */
    private String requireDeposit;

    /**
     * 예약금 금액
     */
    private Integer depositAmount;

    // ========================================
    // 취소 정책
    // ========================================

    /**
     * 예약 취소 허용 (Y/N)
     */
    private String allowCancellation;

    /**
     * 취소 마감 시간 (예약 시간 N시간 전)
     */
    private Integer cancelDeadlineHours;

    /**
     * 노쇼 패널티 사용 (Y/N)
     */
    private String noShowPenaltyEnabled;

    // ========================================
    // 기타
    // ========================================

    /**
     * 타임존
     */
    private String timezone;

    /**
     * 언어
     */
    private String language;
}
