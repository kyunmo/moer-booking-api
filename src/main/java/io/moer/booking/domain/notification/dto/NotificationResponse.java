package io.moer.booking.domain.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.notification.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {
    private Long id;
    private String type;
    private String title;
    private String message;
    private boolean read;
    private String link;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read("Y".equals(notification.getIsRead()))
                .link(notification.getLink())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
