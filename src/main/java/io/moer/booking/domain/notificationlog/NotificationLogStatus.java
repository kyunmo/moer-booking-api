package io.moer.booking.domain.notificationlog;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 발송 상태
 */
@Getter
@RequiredArgsConstructor
public enum NotificationLogStatus {
    PENDING("대기"),
    SENT("발송 완료"),
    FAILED("발송 실패");

    private final String description;
}
