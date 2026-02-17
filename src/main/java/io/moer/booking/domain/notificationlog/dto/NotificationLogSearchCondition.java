package io.moer.booking.domain.notificationlog.dto;

import io.moer.booking.domain.notificationlog.NotificationChannel;
import io.moer.booking.domain.notificationlog.NotificationLogStatus;
import io.moer.booking.domain.notificationlog.NotificationTemplateType;
import lombok.Builder;
import lombok.Getter;

/**
 * 알림 발송 이력 검색 조건
 */
@Getter
@Builder
public class NotificationLogSearchCondition {
    private Long businessId;
    private NotificationChannel channel;
    private NotificationLogStatus status;
    private NotificationTemplateType templateType;
    private int offset;
    private int size;
}
