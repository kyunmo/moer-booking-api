package io.moer.booking.domain.business.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 알림톡 설정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KakaoAlimtalkSettingsRequest {

    private Boolean enabled;

    @Size(max = 100, message = "채널 ID는 100자 이하여야 합니다")
    private String channelId;

    @Size(max = 20, message = "발신 프로필 키는 20자 이하여야 합니다")
    private String senderId;

    @Valid
    private AlimtalkTriggers triggers;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlimtalkTriggers {
        private Boolean onReservationCreated;
        private Boolean onReservationConfirmed;
        private Boolean onReservationCancelled;
        private Boolean onReservationReminder;

        @Min(value = 1, message = "사전 알림 시간은 1시간 이상이어야 합니다")
        private Integer reminderHoursBefore;
    }
}
