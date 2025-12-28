package io.moer.booking.domain.user.service;

import io.moer.booking.common.dto.PageInfo;
import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.user.User;
import io.moer.booking.domain.user.UserStatus;
import io.moer.booking.domain.user.dto.*;
import io.moer.booking.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * 회원 생성
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // User 엔티티 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPassword()) // TODO: 나중에 암호화
                .name(request.getName())
                .phone(request.getPhone())
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();

        // 저장
        userRepository.save(user);

        log.info("User created: id={}, email={}", user.getId(), user.getEmail());

        return UserResponse.from(user);
    }

    /**
     * 회원 단건 조회
     */
    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        return UserResponse.from(user);
    }

    /**
     * 회원 목록 조회 (페이징)
     */
    public PageResponse<UserResponse> getUsers(UserSearchCondition condition) {
        // 데이터 조회
        List<UserResponse> content = userRepository.findAll(condition).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());

        // 전체 개수
        long totalElements = userRepository.countAll(condition);

        // 페이징 정보
        PageInfo pageInfo = new PageInfo(
                condition.getPage(),
                condition.getSize(),
                totalElements
        );

        return PageResponse.of(content, pageInfo);
    }

    /**
     * 회원 수정
     */
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        // 존재 확인
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 수정할 필드만 업데이트
        User updatedUser = User.builder()
                .id(user.getId())
                .name(request.getName() != null ? request.getName() : user.getName())
                .phone(request.getPhone() != null ? request.getPhone() : user.getPhone())
                .password(request.getPassword() != null ? request.getPassword() : user.getPassword())
                .status(user.getStatus())
                .build();

        userRepository.update(updatedUser);

        log.info("User updated: id={}", id);

        // 업데이트된 데이터 재조회
        return getUser(id);
    }

    /**
     * 회원 삭제
     */
    @Transactional
    public void deleteUser(Long id) {
        // 존재 확인
        if (!userRepository.findById(id).isPresent()) {
            throw new EntityNotFoundException(ErrorCode.USER_NOT_FOUND);
        }

        userRepository.delete(id);

        log.info("User deleted: id={}", id);
    }

    /**
     * 회원 상태 변경
     */
    @Transactional
    public UserResponse changeUserStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        User updatedUser = User.builder()
                .id(user.getId())
                .status(status)
                .build();

        userRepository.update(updatedUser);

        log.info("User status changed: id={}, status={}", id, status);

        return getUser(id);
    }
}