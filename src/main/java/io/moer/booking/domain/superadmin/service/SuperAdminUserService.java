package io.moer.booking.domain.superadmin.service;

import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.auditlog.AuditAction;
import io.moer.booking.domain.auditlog.dto.AuditLogCreateRequest;
import io.moer.booking.domain.auditlog.service.AuditLogService;
import io.moer.booking.domain.user.User;
import io.moer.booking.domain.user.UserRole;
import io.moer.booking.domain.user.UserStatus;
import io.moer.booking.domain.user.dto.UserResponse;
import io.moer.booking.domain.user.dto.UserSearchCondition;
import io.moer.booking.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 슈퍼 관리자 - 사용자 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuperAdminUserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    /**
     * 전체 사용자 조회 (페이징, 필터링)
     */
    public PageResponse<UserResponse> getAllUsers(
            UserSearchCondition condition,
            int page,
            int size) {

        int offset = (page - 1) * size;

        List<User> users = userRepository.search(condition);
        long totalElements = userRepository.countSearch(condition);

        List<UserResponse> content = users.stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(content, page, size, (int) totalElements);
    }

    /**
     * 사용자 역할 변경
     */
    @Transactional
    public UserResponse changeUserRole(Long userId, UserRole newRole, User admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        UserRole oldRole = user.getRole();

        // 역할 변경
        User updated = User.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .name(user.getName())
                .phone(user.getPhone())
                .role(newRole)
                .status(user.getStatus())
                .staffId(user.getStaffId())
                .businessId(user.getBusinessId())
                .emailVerified(user.getEmailVerified())
                .trialStartedAt(user.getTrialStartedAt())
                .trialExpiresAt(user.getTrialExpiresAt())
                .isPremium(user.getIsPremium())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();

        userRepository.update(updated);

        // 감사 로그 기록
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userEmail", user.getEmail());
        metadata.put("oldRole", oldRole.name());
        metadata.put("newRole", newRole.name());

        auditLogService.log(AuditLogCreateRequest.builder()
                .userId(admin.getId())
                .userEmail(admin.getEmail())
                .userRole(admin.getRole())
                .action(AuditAction.USER_ROLE_CHANGED.name())
                .entityType("User")
                .entityId(userId)
                .description(String.format("사용자 역할 변경: %s -> %s", oldRole, newRole))
                .metadata(metadata)
                .build());

        log.info("User role changed by SUPER_ADMIN: userId={}, oldRole={}, newRole={}, adminId={}",
                userId, oldRole, newRole, admin.getId());

        return UserResponse.from(updated);
    }

    /**
     * 사용자 강제 정지
     */
    @Transactional
    public UserResponse suspendUser(Long userId, User admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        // SUPER_ADMIN은 정지 불가
        if (user.isSuperAdmin()) {
            throw new BusinessException(
                    ErrorCode.SUPER_ADMIN_CANNOT_BE_DELETED,
                    "슈퍼 관리자는 정지할 수 없습니다");
        }

        // 상태 변경
        userRepository.updateStatus(userId, UserStatus.SUSPENDED);

        // 감사 로그 기록
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userEmail", user.getEmail());
        metadata.put("userRole", user.getRole().name());
        metadata.put("oldStatus", user.getStatus().name());
        metadata.put("newStatus", UserStatus.SUSPENDED.name());

        auditLogService.log(AuditLogCreateRequest.builder()
                .userId(admin.getId())
                .userEmail(admin.getEmail())
                .userRole(admin.getRole())
                .action(AuditAction.USER_STATUS_CHANGED.name())
                .entityType("User")
                .entityId(userId)
                .description("사용자 강제 정지")
                .metadata(metadata)
                .build());

        log.info("User suspended by SUPER_ADMIN: userId={}, adminId={}", userId, admin.getId());

        User updated = userRepository.findById(userId).orElseThrow();
        return UserResponse.from(updated);
    }

    /**
     * 사용자 활성화 (정지 해제)
     */
    @Transactional
    public UserResponse activateUser(Long userId, User admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 이미 활성 상태인 경우
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "이미 활성 상태인 사용자입니다");
        }

        // 상태 변경
        userRepository.updateStatus(userId, UserStatus.ACTIVE);

        // 감사 로그 기록
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userEmail", user.getEmail());
        metadata.put("userRole", user.getRole().name());
        metadata.put("oldStatus", user.getStatus().name());
        metadata.put("newStatus", UserStatus.ACTIVE.name());

        auditLogService.log(AuditLogCreateRequest.builder()
                .userId(admin.getId())
                .userEmail(admin.getEmail())
                .userRole(admin.getRole())
                .action(AuditAction.USER_STATUS_CHANGED.name())
                .entityType("User")
                .entityId(userId)
                .description("사용자 활성화 (정지 해제)")
                .metadata(metadata)
                .build());

        log.info("User activated by SUPER_ADMIN: userId={}, adminId={}", userId, admin.getId());

        User updated = userRepository.findById(userId).orElseThrow();
        return UserResponse.from(updated);
    }

    /**
     * 사용자 강제 삭제
     */
    @Transactional
    public void forceDeleteUser(Long userId, User admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        // SUPER_ADMIN은 삭제 불가
        if (user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_CANNOT_BE_DELETED);
        }

        // 감사 로그 기록
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userEmail", user.getEmail());
        metadata.put("userRole", user.getRole().name());
        metadata.put("userStatus", user.getStatus().name());

        auditLogService.log(AuditLogCreateRequest.builder()
                .userId(admin.getId())
                .userEmail(admin.getEmail())
                .userRole(admin.getRole())
                .action(AuditAction.USER_DELETED.name())
                .entityType("User")
                .entityId(userId)
                .description("사용자 강제 삭제")
                .metadata(metadata)
                .build());

        // 삭제 수행
        userRepository.delete(userId);

        log.info("User force deleted by SUPER_ADMIN: userId={}, adminId={}", userId, admin.getId());
    }
}
