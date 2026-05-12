package io.moer.booking.domain.auth.repository;

import io.moer.booking.domain.auth.RefreshToken;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

/**
 * SECURITY (P1-1): 토큰 회전(rotation) + BCrypt 해시 저장 패턴 사용.
 * - 조회는 user_id 기준. 토큰 문자열로 직접 조회하는 메서드 제거.
 * - 검증은 Service 레이어에서 BCryptPasswordEncoder.matches() 로 수행.
 */
@Mapper
public interface RefreshTokenRepository {

    void save(RefreshToken refreshToken);

    /** 사용자별 최신(가장 최근에 발급된) 리프레시 토큰. */
    Optional<RefreshToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    void deleteExpiredTokens();
}
