package io.moer.booking.domain.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    RESERVATION_NEW("새 예약"),
    RESERVATION_CONFIRMED("예약 확정"),
    RESERVATION_CANCELLED("예약 취소"),
    RESERVATION_COMPLETED("예약 완료"),
    RESERVATION_NO_SHOW("예약 노쇼"),
    SYSTEM("시스템 알림");

    private final String description;
}
