package io.moer.booking.domain.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    private Long id;
    private Long userId;
    private Long businessId;
    private NotificationType type;
    private String title;
    private String message;
    private String link;
    private String referenceType;
    private Long referenceId;
    private String isRead;  // Y/N
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
