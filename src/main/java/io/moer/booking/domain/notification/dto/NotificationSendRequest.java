package io.moer.booking.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "고객 알림 발송 요청")
public class NotificationSendRequest {

    @NotNull(message = "발송 대상 유형은 필수입니다")
    @Schema(description = "발송 대상 유형", example = "ALL", allowableValues = {"ALL", "SPECIFIC", "SEGMENT"})
    private String targetType;

    @Schema(description = "발송 대상 고객 ID 목록 (targetType=SPECIFIC일 때 필수)")
    private List<Long> targetIds;

    @Schema(description = "세그먼트 (targetType=SEGMENT일 때 필수)",
            example = "VIP", allowableValues = {"VIP", "REGULAR", "NEW", "INACTIVE", "BIRTHDAY"})
    private String segment;

    @NotNull(message = "알림 유형은 필수입니다")
    @Schema(description = "알림 유형", example = "NOTICE",
            allowableValues = {"REMINDER", "PROMOTION", "NOTICE", "CUSTOM"})
    private String notificationType;

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 50, message = "제목은 최대 50자까지 입력 가능합니다")
    @Schema(description = "알림 제목", example = "3월 이벤트 안내")
    private String title;

    @NotBlank(message = "메시지 내용은 필수입니다")
    @Size(max = 500, message = "메시지는 최대 500자까지 입력 가능합니다")
    @Schema(description = "알림 메시지", example = "3월 한 달간 전 메뉴 10% 할인 이벤트를 진행합니다.")
    private String message;

    @NotEmpty(message = "발송 채널은 1개 이상 선택해야 합니다")
    @Schema(description = "발송 채널 목록", example = "[\"APP_PUSH\", \"SMS\"]",
            allowableValues = {"APP_PUSH", "KAKAO_ALIMTALK", "SMS"})
    private List<String> channels;

    @Schema(description = "예약 발송 시간 (null이면 즉시 발송)", example = "2026-03-05T10:00:00")
    private LocalDateTime scheduledAt;
}
