package io.moer.booking.domain.auth.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.common.security.JwtTokenProvider;
import io.moer.booking.domain.auth.RefreshToken;
import io.moer.booking.domain.auth.dto.LoginRequest;
import io.moer.booking.domain.auth.dto.LoginResponse;
import io.moer.booking.domain.auth.dto.RefreshTokenRequest;
import io.moer.booking.domain.auth.dto.RegisterRequest;  // 👈 추가
import io.moer.booking.domain.auth.dto.RegisterResponse;  // 👈 추가
import io.moer.booking.domain.auth.repository.RefreshTokenRepository;
import io.moer.booking.domain.business.Business;  // 👈 추가
import io.moer.booking.domain.business.BusinessStatus;  // 👈 추가
import io.moer.booking.domain.business.dto.BusinessResponse;  // 👈 추가
import io.moer.booking.domain.business.repository.BusinessRepository;  // 👈 추가
import io.moer.booking.domain.user.User;
import io.moer.booking.domain.user.UserRole;  // 👈 추가
import io.moer.booking.domain.user.UserStatus;  // 👈 추가
import io.moer.booking.domain.user.dto.UserResponse;  // 👈 추가
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
    private final BusinessRepository businessRepository;  // 👈 추가
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

        // 7. 응답 생성
        return LoginResponse.of(accessToken, refreshToken, 3600L, user);
    }

    /**
     * 토큰 갱신
     */
    @Transactional
    public LoginResponse refreshAccessToken(RefreshTokenRequest request) {
        // 1. Refresh Token 조회
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        // 2. 토큰 만료 확인
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.deleteByToken(request.getRefreshToken());
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        }

        // 3. 사용자 조회
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 4. 새 Access Token 생성
        String newAccessToken = tokenProvider.generateAccessToken(user);

        log.info("Access token refreshed: userId={}", user.getId());

        // 5. 응답 (기존 Refresh Token 유지)
        return LoginResponse.of(newAccessToken, request.getRefreshToken(), 3600L, user);
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("User logged out: userId={}", userId);
    }

    /**
     * 🆕 회원가입 (사용자 + 매장 통합 생성)
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // 1. 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 2. User 생성
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .name(request.getName())
                .phone(request.getPhone())
                .role(UserRole.OWNER)
                .status(UserStatus.ACTIVE)
                .emailVerified("N")
                .build();

        userRepository.save(user);
        log.info("User created: id={}, email={}", user.getId(), user.getEmail());

        // 3. Business 생성
        Business business = Business.builder()
                .ownerId(user.getId())
                .name(request.getBusinessName())
                .businessType(request.getBusinessType())
                .status(BusinessStatus.ACTIVE)
                .build();

        businessRepository.save(business);
        log.info("Business created: id={}, name={}, ownerId={}",
                business.getId(), business.getName(), user.getId());

        // 4. User에 businessId 업데이트
        userRepository.updateBusinessId(user.getId(), business.getId());
        user.updateBusinessId(business.getId());

        // 5. JWT 토큰 생성
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        // 6. Refresh Token 저장
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        // 7. 응답 생성
        UserResponse userResponse = UserResponse.from(user);
        BusinessResponse businessResponse = BusinessResponse.from(business);

        log.info("Registration completed: userId={}, businessId={}", user.getId(), business.getId());

        return RegisterResponse.of(
                accessToken,
                refreshToken,
                3600L,
                userResponse,
                businessResponse
        );
    }
}