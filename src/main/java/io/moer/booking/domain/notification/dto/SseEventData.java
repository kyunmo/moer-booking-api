package io.moer.booking.domain.notification.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SseEventData {
    private String type;
    private Long referenceId;
    private String reservationNumber;
    private String customerName;
    private String serviceName;
    private String staffName;
    private String startTime;
    private String reason;
    private Integer rating;
    private String contentPreview;
    private String message;
    private LocalDateTime createdAt;
}
