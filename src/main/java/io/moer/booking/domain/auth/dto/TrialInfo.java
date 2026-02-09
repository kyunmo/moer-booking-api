package io.moer.booking.domain.auth.dto;

import io.moer.booking.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 체험판 정보 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrialInfo {
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private int remainingDays;
    private boolean isPremium;

    public static TrialInfo from(User user) {
        return TrialInfo.builder()
                .startedAt(user.getTrialStartedAt())
                .expiresAt(user.getTrialExpiresAt())
                .remainingDays(user.getRemainingTrialDays())
                .isPremium(user.isPremiumUser())
                .build();
    }
}
