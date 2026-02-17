package io.moer.booking.domain.notificationlog;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 템플릿 타입
 */
@Getter
@RequiredArgsConstructor
public enum NotificationTemplateType {
    RESERVATION_CREATED("예약 생성"),
    RESERVATION_CONFIRMED("예약 확정"),
    RESERVATION_REMINDER("예약 리마인더"),
    RESERVATION_CHANGED("예약 변경"),
    RESERVATION_CANCELLED("예약 취소"),
    REVIEW_REQUEST("리뷰 요청");

    private final String description;
}
