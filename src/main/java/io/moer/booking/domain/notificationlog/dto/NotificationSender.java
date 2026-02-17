package io.moer.booking.domain.notificationlog.dto;

import java.util.Map;

/**
 * 외부 알림 발송 인터페이스
 * <p>
 * 실제 카카오톡/SMS 연동 시 이 인터페이스를 구현하여 교체한다.
 * 현재는 LogNotificationSender가 DB 로그만 기록하는 구현체.
 */
public interface NotificationSender {

    /**
     * 예약 생성 알림 발송
     */
    void sendReservationCreated(Long businessId, Long reservationId,
                                 String recipientPhone, String recipientName,
                                 Map<String, String> params);

    /**
     * 예약 확정 알림 발송
     */
    void sendReservationConfirmed(Long businessId, Long reservationId,
                                   String recipientPhone, String recipientName,
                                   Map<String, String> params);

    /**
     * 예약 취소 알림 발송
     */
    void sendReservationCancelled(Long businessId, Long reservationId,
                                   String recipientPhone, String recipientName,
                                   Map<String, String> params);

    /**
     * 리뷰 요청 알림 발송
     */
    void sendReviewRequest(Long businessId, Long reservationId,
                            String recipientPhone, String recipientName,
                            Map<String, String> params);
}
