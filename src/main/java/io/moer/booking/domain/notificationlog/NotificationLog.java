package io.moer.booking.domain.notificationlog;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 알림 발송 기록 (카카오/SMS/이메일 등 외부 알림)
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NotificationLog {

    private Long id;
    private Long businessId;
    private Long reservationId;
    private NotificationChannel channel;
    private NotificationTemplateType templateType;
    private String recipientPhone;
    private String recipientName;
    private String title;
    private String content;
    private NotificationLogStatus status;
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;

    /**
     * 발송 성공 처리
     */
    public void markAsSent() {
        this.status = NotificationLogStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * 발송 실패 처리
     */
    public void markAsFailed(String errorMessage) {
        this.status = NotificationLogStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /**
     * 발송 완료 여부
     */
    public boolean isSent() {
        return this.status == NotificationLogStatus.SENT;
    }

    /**
     * 발송 실패 여부
     */
    public boolean isFailed() {
        return this.status == NotificationLogStatus.FAILED;
    }
}
