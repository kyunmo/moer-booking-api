package io.moer.booking.domain.auth.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.common.security.JwtTokenProvider;
import io.moer.booking.domain.auth.RefreshToken;
import io.moer.booking.domain.auth.dto.LoginRequest;
import io.moer.booking.domain.auth.dto.LoginResponse;
import io.moer.booking.domain.auth.dto.RefreshTokenRequest;
import io.moer.booking.domain.auth.repository.RefreshTokenRepository;
import io.moer.booking.domain.user.User;
import io.moer.booking.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    /**
     * 로그인
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 3. 계정 상태 확인
        if (user.getStatus().name().equals("INACTIVE")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "휴면 계정입니다");
        }
        if (user.getStatus().name().equals("SUSPENDED")) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "정지된 계정입니다");
        }

        // 4. JWT 토큰 생성
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        // 5. Refresh Token 저장 (기존 것 삭제 후 저장)
        refreshTokenRepository.deleteByUserId(user.getId());

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        // 6. 마지막 로그인 시간 업데이트
        userRepository.updateLastLoginAt(user.getId(), LocalDateTime.now());

        log.info("User logged in: email={}, userId={}, role={}",
                user.getEmail(), user.getId(), user.getRole());

        // 7. 응답 생성 (expiresIn은 초 단위)
        return LoginResponse.of(
                accessToken,
                refreshToken,
                3600L,  // 1시간 = 3600초
                user
        );
    }

    /**
     * 토큰 갱신
     */
    @Transactional
    public LoginResponse refreshAccessToken(RefreshTokenRequest request) {
        String refreshTokenStr = request.getRefreshToken();

        // 1. Refresh Token 검증
        if (!tokenProvider.validateToken(refreshTokenStr)) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN, "Refresh Token이 유효하지 않습니다");
        }

        // 2. DB에서 Refresh Token 조회
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.TOKEN_NOT_FOUND, "Refresh Token을 찾을 수 없습니다"));

        // 3. 만료 확인
        if (refreshToken.isExpired()) {
            refreshTokenRepository.deleteByToken(refreshTokenStr);
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN, "Refresh Token이 만료되었습니다");
        }

        // 4. 사용자 조회
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 5. 새 Access Token 생성
        String newAccessToken = tokenProvider.generateAccessToken(user);

        log.info("Access token refreshed: userId={}", user.getId());

        // 6. 응답 (Refresh Token은 그대로 유지)
        return LoginResponse.of(
                newAccessToken,
                refreshTokenStr,
                3600L,
                user
        );
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("User logged out: userId={}", userId);
    }
}