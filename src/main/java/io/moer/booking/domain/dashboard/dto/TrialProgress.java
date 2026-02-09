package io.moer.booking.domain.dashboard.dto;

import io.moer.booking.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대시보드용 체험판 진행 상황 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrialProgress {
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private int totalDays;       // 30
    private int remainingDays;
    private int usedDays;
    private boolean isPremium;
    private boolean isExpired;

    public static TrialProgress from(User owner) {
        if (owner.isPremiumUser()) {
            return TrialProgress.builder()
                    .isPremium(true)
                    .isExpired(false)
                    .totalDays(0)
                    .remainingDays(0)
                    .usedDays(0)
                    .build();
        }

        int remaining = owner.getRemainingTrialDays();
        return TrialProgress.builder()
                .startedAt(owner.getTrialStartedAt())
                .expiresAt(owner.getTrialExpiresAt())
                .totalDays(30)
                .remainingDays(remaining)
                .usedDays(30 - remaining)
                .isPremium(false)
                .isExpired(owner.isTrialExpired())
                .build();
    }
}
