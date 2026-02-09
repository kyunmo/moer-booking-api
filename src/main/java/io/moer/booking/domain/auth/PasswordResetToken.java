package io.moer.booking.domain.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 비밀번호 재설정 토큰 엔티티
 * DB 테이블: password_reset_tokens
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {
    private Long id;
    private Long userId;
    private String token;  // UUID
    private LocalDateTime expiresAt;
    private String used;  // Y/N
    private LocalDateTime createdAt;

    /**
     * 토큰이 만료되었는지 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 토큰이 사용되었는지 확인
     */
    public boolean isUsed() {
        return "Y".equals(used);
    }
}
