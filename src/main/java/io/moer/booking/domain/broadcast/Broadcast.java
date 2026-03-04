package io.moer.booking.domain.broadcast;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Broadcast {
    private Long id;
    private String title;
    private String content;
    private String targetType; // ALL, PAID, TRIAL, FREE
    private String priority; // LOW, NORMAL, HIGH, URGENT
    private Long sentBy;
    private LocalDateTime sentAt;
    private String status; // DRAFT, SENT
    private Integer recipientCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
