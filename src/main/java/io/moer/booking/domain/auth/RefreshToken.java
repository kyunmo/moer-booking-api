package io.moer.booking.domain.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 리프레시 토큰.
 *
 * SECURITY (P1-1): 토큰 문자열은 평문이 아닌 BCrypt 해시로 저장.
 * - user_id 로 조회 → BCrypt.matches(rawToken, tokenHash) 로 검증
 * - 매 갱신 시 새 해시로 교체 (회전)
 * - 재사용(이미 회전된 토큰 재제출) 감지 시 전체 세션 무효화
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    private Long id;
    private Long userId;
    /** BCrypt 해시된 토큰 문자열. 원본 토큰은 저장하지 않음. */
    private String tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
