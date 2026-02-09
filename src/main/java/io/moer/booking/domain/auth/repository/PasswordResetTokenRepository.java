package io.moer.booking.domain.auth.repository;

import io.moer.booking.domain.auth.PasswordResetToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 비밀번호 재설정 토큰 Repository
 */
@Mapper
public interface PasswordResetTokenRepository {

    /**
     * 토큰 저장
     */
    void save(PasswordResetToken token);

    /**
     * 토큰으로 조회
     */
    Optional<PasswordResetToken> findByToken(@Param("token") String token);

    /**
     * 사용자 ID로 미사용 토큰 삭제
     */
    void deleteUnusedByUserId(@Param("userId") Long userId);

    /**
     * 토큰을 사용 완료로 표시
     */
    void markAsUsed(@Param("token") String token);

    /**
     * 만료된 토큰 삭제 (배치 작업용)
     */
    void deleteExpired();
}
