package io.moer.booking.domain.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SNS 계정 연동 엔티티
 * DB 테이블: sns_accounts
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnsAccount {
    private Long id;
    private Long userId;
    private SnsProvider provider;
    private String providerUserId;
    private String email;
    private String name;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
