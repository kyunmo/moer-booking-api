package io.moer.booking.domain.auth.repository;

import io.moer.booking.domain.auth.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface RefreshTokenRepository {

    void save(RefreshToken refreshToken);

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    void deleteByToken(String token);

    void deleteExpiredTokens();
}