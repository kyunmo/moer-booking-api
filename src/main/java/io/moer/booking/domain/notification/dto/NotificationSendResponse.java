package io.moer.booking.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "고객 알림 발송 응답")
public class NotificationSendResponse {

    @Schema(description = "알림 발송 로그 ID (첫 번째 로그)")
    private Long notificationLogId;

    @Schema(description = "발송 대상 고객 수")
    private int targetCount;

    @Schema(description = "예약 발송 시간 (즉시 발송이면 null)")
    private LocalDateTime scheduledAt;

    @Schema(description = "실제 발송 시간")
    private LocalDateTime sentAt;

    @Schema(description = "발송 상태", example = "SENT")
    private String status;

    @Schema(description = "발송 채널 목록")
    private List<String> channels;

    @Schema(description = "발송 성공 건수")
    private int successCount;

    @Schema(description = "발송 실패 건수")
    private int failCount;
}
