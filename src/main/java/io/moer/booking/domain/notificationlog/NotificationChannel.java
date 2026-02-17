package io.moer.booking.domain.notificationlog;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 발송 채널
 */
@Getter
@RequiredArgsConstructor
public enum NotificationChannel {
    KAKAO("카카오톡"),
    SMS("문자메시지"),
    EMAIL("이메일");

    private final String description;
}
