package io.moer.booking.domain.notificationlog.dto;

import io.moer.booking.domain.notificationlog.NotificationChannel;
import io.moer.booking.domain.notificationlog.NotificationLog;
import io.moer.booking.domain.notificationlog.NotificationLogStatus;
import io.moer.booking.domain.notificationlog.NotificationTemplateType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 알림 발송 이력 응답 DTO
 */
@Getter
@Builder
@Schema(description = "알림 발송 이력 응답")
public class NotificationLogResponse {

    @Schema(description = "알림 로그 ID", example = "1")
    private Long id;

    @Schema(description = "매장 ID", example = "1")
    private Long businessId;

    @Schema(description = "예약 ID", example = "10")
    private Long reservationId;

    @Schema(description = "발송 채널", example = "KAKAO")
    private NotificationChannel channel;

    @Schema(description = "템플릿 타입", example = "RESERVATION_CREATED")
    private NotificationTemplateType templateType;

    @Schema(description = "수신자 전화번호 (마스킹)", example = "010-****-5678")
    private String recipientPhone;

    @Schema(description = "수신자 이름", example = "홍길동")
    private String recipientName;

    @Schema(description = "알림 제목", example = "예약이 생성되었습니다")
    private String title;

    @Schema(description = "알림 내용")
    private String content;

    @Schema(description = "발송 상태", example = "SENT")
    private NotificationLogStatus status;

    @Schema(description = "오류 메시지")
    private String errorMessage;

    @Schema(description = "발송 시각")
    private LocalDateTime sentAt;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    /**
     * Entity -> Response DTO 변환
     */
    public static NotificationLogResponse from(NotificationLog entity) {
        return NotificationLogResponse.builder()
                .id(entity.getId())
                .businessId(entity.getBusinessId())
                .reservationId(entity.getReservationId())
                .channel(entity.getChannel())
                .templateType(entity.getTemplateType())
                .recipientPhone(maskPhone(entity.getRecipientPhone()))
                .recipientName(entity.getRecipientName())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus())
                .errorMessage(entity.getErrorMessage())
                .sentAt(entity.getSentAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * 전화번호 마스킹 처리
     * 010-1234-5678 -> 010-****-5678
     * 01012345678 -> 010****5678
     */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) {
            return phone;
        }

        // 하이픈이 포함된 경우 (010-1234-5678)
        if (phone.contains("-")) {
            String[] parts = phone.split("-");
            if (parts.length == 3) {
                return parts[0] + "-****-" + parts[2];
            }
        }

        // 하이픈 없는 경우 (01012345678)
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }

        // 기타 형식은 중간 4자리 마스킹
        int start = phone.length() / 2 - 2;
        return phone.substring(0, start) + "****" + phone.substring(start + 4);
    }
}
