package io.moer.booking.domain.auth.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.common.security.JwtTokenProvider;
import io.moer.booking.common.storage.FileStorageService;
import io.moer.booking.domain.auditlog.AuditAction;
import io.moer.booking.domain.auditlog.service.AuditLogService;
import io.moer.booking.domain.auth.PasswordResetToken;
import io.moer.booking.domain.auth.RefreshToken;
import io.moer.booking.domain.auth.SnsAccount;
import io.moer.booking.domain.auth.SnsProvider;
import io.moer.booking.domain.auth.dto.*;
import io.moer.booking.domain.auth.repository.PasswordResetTokenRepository;
import io.moer.booking.domain.auth.repository.RefreshTokenRepository;
import io.moer.booking.domain.auth.repository.SnsAccountRepository;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.BusinessStatus;
import io.moer.booking.domain.business.dto.BusinessResponse;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.reservation.Reservation;
import io.moer.booking.domain.reservation.ReservationStatus;
import io.moer.booking.domain.reservation.repository.ReservationRepository;
import io.moer.booking.domain.staff.repository.StaffRepository;
import io.moer.booking.domain.user.User;
import io.moer.booking.domain.user.UserRole;
import io.moer.booking.domain.user.UserStatus;
import io.moer.booking.domain.user.dto.UserResponse;
import io.moer.booking.domain.user.repository.UserRepository;
import io.moer.booking.common.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BusinessRepository businessRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SnsAccountRepository snsAccountRepository;
    private final StaffRepository staffRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final FileStorageService fileStorageService;

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
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "탈퇴된 계정입니다");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "휴면 계정입니다");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
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

        // 2. User 생성 (30일 체험판 자동 설정, 항상 OWNER 역할)
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        LocalDateTime now = LocalDateTime.now();

        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .name(request.getName())
                .phone(request.getPhone())
                .role(UserRole.OWNER)
                .status(UserStatus.ACTIVE)
                .emailVerified("N")
                .trialStartedAt(now)
                .trialExpiresAt(now.plusDays(30))
                .isPremium("N")
                .build();

        userRepository.save(user);
        log.info("User created: id={}, email={}", user.getId(), user.getEmail());

        // 3. Business 생성 (선택한 플랜 + 30일 무료 체험 자동 설정)
        Business business = Business.builder()
                .ownerId(user.getId())
                .name(request.getBusinessName())
                .businessType(request.getBusinessType())
                .status(BusinessStatus.ACTIVE)
                .subscriptionPlan(request.getSubscriptionPlan())
                .subscriptionStatus(io.moer.booking.domain.business.SubscriptionStatus.TRIAL)
                .trialStartedAt(now)
                .trialEndsAt(now.plusDays(30))
                .currentStaffCount(0)
                .currentMonthReservationCount(0)
                .build();

        businessRepository.save(business);
        log.info("Business created: id={}, name={}, ownerId={}",
                business.getId(), business.getName(), user.getId());

        // 4. User에 businessId 업데이트
        userRepository.updateBusinessId(user.getId(), business.getId());
        user.updateBusinessId(business.getId());

        // 5. 감사 로그 기록 (회원가입)
        Map<String, Object> userMetadata = new HashMap<>();
        userMetadata.put("userEmail", user.getEmail());
        userMetadata.put("userName", user.getName());
        userMetadata.put("businessName", business.getName());
        userMetadata.put("businessType", business.getBusinessType().name());
        userMetadata.put("trialExpiresAt", user.getTrialExpiresAt().toString());

        auditLogService.log(
                user,
                AuditAction.USER_CREATED,
                "User",
                user.getId(),
                String.format("회원가입: %s (%s) - 매장: %s", user.getName(), user.getEmail(), business.getName()),
                userMetadata
        );

        Map<String, Object> businessMetadata = new HashMap<>();
        businessMetadata.put("businessName", business.getName());
        businessMetadata.put("businessType", business.getBusinessType().name());
        businessMetadata.put("ownerId", user.getId());
        businessMetadata.put("ownerEmail", user.getEmail());
        businessMetadata.put("subscriptionPlan", business.getSubscriptionPlan().name());
        businessMetadata.put("subscriptionStatus", business.getSubscriptionStatus().name());
        businessMetadata.put("trialEndsAt", business.getTrialEndsAt().toString());

        auditLogService.log(
                user,
                AuditAction.BUSINESS_CREATED,
                "Business",
                business.getId(),
                String.format("회원가입 시 매장 생성: %s (업종: %s)", business.getName(), business.getBusinessType().getDescription()),
                businessMetadata
        );

        // 6. JWT 토큰 생성
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        // 7. Refresh Token 저장
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        // 8. 응답 생성 (체험판 정보 포함)
        UserResponse userResponse = UserResponse.from(user);
        BusinessResponse businessResponse = BusinessResponse.from(business);
        TrialInfo trialInfo = TrialInfo.from(user);

        log.info("Registration completed: userId={}, businessId={}, trial expires at: {}",
                user.getId(), business.getId(), user.getTrialExpiresAt());

        return RegisterResponse.of(
                accessToken,
                refreshToken,
                3600L,
                userResponse,
                businessResponse,
                trialInfo
        );
    }

    /**
     * 🆕 비밀번호 찾기 요청
     */
    @Transactional
    public void requestPasswordReset(String email) {
        // 1. 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "해당 이메일로 등록된 사용자를 찾을 수 없습니다: " + email
                ));

        // 2. 기존 미사용 토큰 삭제 (중복 방지)
        passwordResetTokenRepository.deleteUnusedByUserId(user.getId());

        // 3. 새 토큰 생성 (UUID, 30분 유효)
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used("N")
                .build();
        passwordResetTokenRepository.save(resetToken);

        // 4. 비밀번호 재설정 이메일 발송 (비동기)
        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);

        log.info("Password reset requested: userId={}, email={}, tokenId={}",
                user.getId(), user.getEmail(), resetToken.getId());
    }

    /**
     * 🆕 비밀번호 재설정 실행
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // 1. 토큰 조회
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESET_TOKEN_INVALID,
                        "유효하지 않은 비밀번호 재설정 토큰입니다"
                ));

        // 2. 토큰 검증
        if (resetToken.isUsed()) {
            throw new BusinessException(
                    ErrorCode.RESET_TOKEN_USED,
                    "이미 사용된 비밀번호 재설정 토큰입니다"
            );
        }
        if (resetToken.isExpired()) {
            throw new BusinessException(
                    ErrorCode.RESET_TOKEN_EXPIRED,
                    "만료된 비밀번호 재설정 토큰입니다. 다시 요청해주세요."
            );
        }

        // 3. 사용자 조회
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 4. 비밀번호 변경
        String encodedPassword = passwordEncoder.encode(newPassword);
        userRepository.updatePassword(user.getId(), encodedPassword);

        // 5. 토큰 사용 처리
        passwordResetTokenRepository.markAsUsed(token);

        // 6. 모든 Refresh Token 삭제 (재로그인 강제)
        refreshTokenRepository.deleteByUserId(user.getId());

        log.info("Password reset completed: userId={}, email={}", user.getId(), user.getEmail());
    }

    /**
     * 비밀번호 변경 (로그인한 사용자)
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 2. 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 3. 새 비밀번호 확인 일치 체크
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        // 4. 현재 비밀번호와 동일한지 체크
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.SAME_PASSWORD);
        }

        // 5. 비밀번호 변경
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        userRepository.updatePassword(user.getId(), encodedPassword);

        // 6. 모든 Refresh Token 삭제 (재로그인 강제)
        refreshTokenRepository.deleteByUserId(user.getId());

        // 7. 감사 로그
        auditLogService.log(user, AuditAction.USER_PASSWORD_CHANGED,
                "User", user.getId(), "비밀번호 변경", null);

        log.info("Password changed: userId={}", userId);
    }

    /**
     * 프로필 수정 (이름, 전화번호)
     */
    @Transactional
    public UserResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 2. 변경할 값 결정 (null이면 기존값 유지)
        String newName = request.getName() != null ? request.getName() : user.getName();
        String newPhone = request.getPhone() != null ? request.getPhone() : user.getPhone();

        // 3. 업데이트
        userRepository.updateProfile(user.getId(), newName, newPhone);

        // 4. 감사 로그
        Map<String, Object> metadata = new HashMap<>();
        if (request.getName() != null) metadata.put("name", newName);
        if (request.getPhone() != null) metadata.put("phone", newPhone);
        auditLogService.log(user, AuditAction.USER_PROFILE_UPDATED,
                "User", user.getId(), "프로필 수정", metadata);

        // 5. 변경된 사용자 조회 후 반환
        User updatedUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(updatedUser);
    }

    /**
     * 프로필 이미지 업로드
     */
    @Transactional
    public ProfileImageResponse uploadProfileImage(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 기존 이미지 삭제
        if (user.getProfileImageUrl() != null) {
            fileStorageService.delete(user.getProfileImageUrl());
        }

        // 새 이미지 저장
        String imageUrl = fileStorageService.store(file, "profiles");

        // DB 업데이트
        userRepository.updateProfileImageUrl(user.getId(), imageUrl);

        log.info("Profile image uploaded: userId={}, url={}", userId, imageUrl);
        return new ProfileImageResponse(imageUrl);
    }

    /**
     * SNS 연결 계정 목록 조회
     */
    public List<SnsAccountResponse> getSocialAccounts(Long userId) {
        List<SnsAccount> accounts = snsAccountRepository.findByUserId(userId);
        return accounts.stream()
                .map(SnsAccountResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * SNS 계정 연결 해제
     */
    @Transactional
    public void disconnectSocialAccount(Long userId, String providerName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        SnsProvider provider;
        try {
            provider = SnsProvider.valueOf(providerName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.OAUTH2_PROVIDER_NOT_FOUND,
                    "지원하지 않는 SNS 제공자입니다: " + providerName);
        }

        // 연결된 계정 존재 확인
        snsAccountRepository.findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new BusinessException(ErrorCode.SNS_ACCOUNT_NOT_CONNECTED));

        // 마지막 로그인 수단 체크
        int snsCount = snsAccountRepository.countByUserId(userId);
        if (snsCount <= 1) {
            throw new BusinessException(ErrorCode.LAST_LOGIN_METHOD_CANNOT_REMOVE,
                    "마지막 SNS 연결입니다. 비밀번호를 먼저 설정해주세요.");
        }

        // 삭제
        snsAccountRepository.deleteByUserIdAndProvider(userId, provider);

        // 감사 로그
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", provider.name());
        auditLogService.log(user, AuditAction.SNS_ACCOUNT_DISCONNECTED,
                "SnsAccount", null, provider.name() + " 계정 연결 해제", metadata);

        log.info("SNS account disconnected: userId={}, provider={}", userId, provider);
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void deleteAccount(Long userId, DeleteAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 1. SUPER_ADMIN 탈퇴 차단
        if (user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_CANNOT_BE_DELETED);
        }

        // 2. 이미 탈퇴된 계정 체크
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_DELETED);
        }

        // 3. 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 4. 진행 중인 예약 확인 (OWNER인 경우 매장 예약, STAFF인 경우 본인 예약)
        if (user.getBusinessId() != null) {
            LocalDate today = LocalDate.now();
            List<Reservation> activeReservations;
            if (user.isOwner()) {
                // OWNER: 매장의 PENDING/CONFIRMED 예약 중 오늘 이후
                activeReservations = reservationRepository.findByBusinessIdAndDateRange(
                        user.getBusinessId(), today, today.plusYears(1));
                activeReservations = activeReservations.stream()
                        .filter(r -> r.getStatus() == ReservationStatus.PENDING ||
                                     r.getStatus() == ReservationStatus.CONFIRMED)
                        .collect(Collectors.toList());
            } else {
                // STAFF: 본인 담당 PENDING/CONFIRMED 예약
                activeReservations = reservationRepository.findByStaffId(
                        user.getStaffId() != null ? user.getStaffId() : -1L);
                activeReservations = activeReservations.stream()
                        .filter(r -> r.getStatus() == ReservationStatus.PENDING ||
                                     r.getStatus() == ReservationStatus.CONFIRMED)
                        .filter(r -> !r.getReservationDate().isBefore(today))
                        .collect(Collectors.toList());
            }

            if (!activeReservations.isEmpty()) {
                throw new BusinessException(ErrorCode.ACCOUNT_DELETE_HAS_ACTIVE_RESERVATIONS,
                        "진행 중인 예약 " + activeReservations.size() + "건이 있습니다. 예약을 처리한 후 탈퇴해주세요.");
            }
        }

        // 5. OWNER 탈퇴 시 매장 비활성화
        if (user.isOwner() && user.getBusinessId() != null) {
            businessRepository.updateStatus(user.getBusinessId(), BusinessStatus.INACTIVE);
            // 매장 소속 직원 비활성화
            staffRepository.deactivateByBusinessId(user.getBusinessId());
        }

        // 6. STAFF 탈퇴 시 스태프 비활성화
        if (user.isStaff() && user.getStaffId() != null) {
            staffRepository.deactivate(user.getStaffId());
        }

        // 7. 사용자 상태 DELETED로 변경 (Soft Delete)
        userRepository.updateStatus(user.getId(), UserStatus.DELETED);

        // 8. 모든 Refresh Token 삭제
        refreshTokenRepository.deleteByUserId(user.getId());

        // 9. 모든 SNS 계정 연결 해제
        snsAccountRepository.deleteByUserId(user.getId());

        // 10. 감사 로그
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reason", request.getReason());
        metadata.put("role", user.getRole().name());
        if (user.getBusinessId() != null) metadata.put("businessId", user.getBusinessId());
        auditLogService.log(user, AuditAction.USER_ACCOUNT_DELETED,
                "User", user.getId(), "회원 탈퇴: " + user.getEmail(), metadata);

        log.info("Account deleted: userId={}, email={}, role={}", userId, user.getEmail(), user.getRole());
    }
}