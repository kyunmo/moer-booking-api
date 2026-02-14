package io.moer.booking.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class NotificationListResponse {
    private List<NotificationResponse> items;
    private int totalCount;
    private int unreadCount;
}
