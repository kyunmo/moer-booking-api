package io.moer.booking.domain.business.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 카카오 알림톡 설정 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KakaoAlimtalkSettingsResponse {

    private boolean enabled;
    private String channelId;
    private String senderId;
    private AlimtalkTriggers triggers;
    private LocalDateTime verifiedAt;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlimtalkTriggers {
        private boolean onReservationCreated;
        private boolean onReservationConfirmed;
        private boolean onReservationCancelled;
        private boolean onReservationReminder;
        private int reminderHoursBefore;
    }

    /**
     * 기본값 응답 (설정 미존재 시)
     */
    public static KakaoAlimtalkSettingsResponse defaults() {
        return KakaoAlimtalkSettingsResponse.builder()
                .enabled(false)
                .channelId(null)
                .senderId(null)
                .triggers(AlimtalkTriggers.builder()
                        .onReservationCreated(true)
                        .onReservationConfirmed(true)
                        .onReservationCancelled(true)
                        .onReservationReminder(true)
                        .reminderHoursBefore(24)
                        .build())
                .verifiedAt(null)
                .build();
    }
}
