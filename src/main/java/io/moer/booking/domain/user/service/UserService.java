package io.moer.booking.domain.user.service;

import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.auditlog.AuditAction;
import io.moer.booking.domain.auditlog.service.AuditLogService;
import io.moer.booking.domain.user.User;
import io.moer.booking.domain.user.UserRole;
import io.moer.booking.domain.user.UserStatus;
import io.moer.booking.domain.user.dto.UserCreateRequest;
import io.moer.booking.domain.user.dto.UserResponse;
import io.moer.booking.domain.user.dto.UserSearchCondition;
import io.moer.booking.domain.user.dto.UserUpdateRequest;
import io.moer.booking.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional
    public UserResponse createUser(UserCreateRequest request, User currentUser) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .name(request.getName())
                .phone(request.getPhone())
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .staffId(request.getStaffId())
                .businessId(request.getBusinessId())
                .emailVerified("N")  // boolean → String 'N'
                .build();

        userRepository.save(user);

        // 감사 로그 기록
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userEmail", user.getEmail());
        metadata.put("userName", user.getName());
        metadata.put("userRole", user.getRole().name());
        if (user.getBusinessId() != null) {
            metadata.put("businessId", user.getBusinessId());
        }

        auditLogService.log(
                currentUser,
                AuditAction.USER_CREATED,
                "User",
                user.getId(),
                String.format("사용자 생성: %s (%s)", user.getName(), user.getEmail()),
                metadata
        );

        log.info("User created: id={}, email={}, role={}",
                user.getId(), user.getEmail(), user.getRole());

        return UserResponse.from(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    public UserResponse getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        User updatedUser = User.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .name(request.getName() != null ? request.getName() : user.getName())
                .phone(request.getPhone() != null ? request.getPhone() : user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .staffId(request.getStaffId() != null ? request.getStaffId() : user.getStaffId())
                .businessId(request.getBusinessId() != null ? request.getBusinessId() : user.getBusinessId())
                .emailVerified(user.getEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .build();

        userRepository.update(updatedUser);

        log.info("User updated: id={}, email={}", userId, user.getEmail());

        return getUser(userId);
    }

    @Transactional
    public UserResponse updateUserStatus(Long userId, UserStatus status, User currentUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        UserStatus oldStatus = user.getStatus();

        userRepository.updateStatus(userId, status);

        // 감사 로그 기록
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userEmail", user.getEmail());
        metadata.put("userName", user.getName());
        metadata.put("oldStatus", oldStatus.name());
        metadata.put("newStatus", status.name());

        auditLogService.log(
                currentUser,
                AuditAction.USER_STATUS_CHANGED,
                "User",
                userId,
                String.format("사용자 상태 변경: %s (%s → %s)", user.getName(), oldStatus, status),
                metadata
        );

        log.info("User status updated: id={}, status={}", userId, status);

        return getUser(userId);
    }

    public boolean checkEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public PageResponse<UserResponse> searchUsers(UserSearchCondition condition) {
        List<User> users = userRepository.search(condition);
        long totalElements = userRepository.countSearch(condition);

        List<UserResponse> content = users.stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(content, condition.getPage(), condition.getSize(), totalElements);
    }
}