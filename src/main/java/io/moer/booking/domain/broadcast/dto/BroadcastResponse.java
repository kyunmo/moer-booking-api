package io.moer.booking.domain.broadcast.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.broadcast.Broadcast;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class BroadcastResponse {
    private Long id;
    private String title;
    private String content;
    private String targetType;
    private String priority;
    private String status;
    private Integer recipientCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static BroadcastResponse from(Broadcast broadcast) {
        return BroadcastResponse.builder()
                .id(broadcast.getId())
                .title(broadcast.getTitle())
                .content(broadcast.getContent())
                .targetType(broadcast.getTargetType())
                .priority(broadcast.getPriority())
                .status(broadcast.getStatus())
                .recipientCount(broadcast.getRecipientCount())
                .sentAt(broadcast.getSentAt())
                .createdAt(broadcast.getCreatedAt())
                .build();
    }
}
