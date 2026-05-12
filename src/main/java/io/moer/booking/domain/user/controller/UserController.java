package io.moer.booking.domain.user.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.user.UserStatus;
import io.moer.booking.domain.user.dto.UserResponse;
import io.moer.booking.domain.user.dto.UserSearchCondition;
import io.moer.booking.domain.user.dto.UserUpdateRequest;
import io.moer.booking.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 사용자 관리 API.
 *
 * SECURITY (P0-3):
 * - 모든 목록/검색/이메일 조회는 ADMIN/SUPER_ADMIN 전용 (IDOR / 사용자 열거 방어).
 * - 단건 조회/수정은 본인 또는 관리자.
 * - 상태 변경은 관리자 전용.
 * - 이메일 중복 확인(check-email)은 회원가입 플로우용으로 비인증 유지하되,
 *   별도로 Rate Limiting 적용 예정(P1-3).
 */
@Tag(name = "사용자", description = "사용자 관리 API (조회, 수정, 검색)")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "사용자 목록 조회",
            description = "전체 사용자 목록을 조회합니다. (ADMIN/SUPER_ADMIN 전용)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping
    public ApiResponse<List<UserResponse>> getUsers() {
        List<UserResponse> response = userService.getAllUsers();
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "사용자 단건 조회",
            description = "ID로 사용자를 조회합니다. 본인 또는 ADMIN/SUPER_ADMIN 만 가능."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or #userId == principal.userId")
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long userId) {
        UserResponse response = userService.getUser(userId);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "이메일로 사용자 조회",
            description = "이메일로 사용자를 조회합니다. (ADMIN/SUPER_ADMIN 전용)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/email/{email}")
    public ApiResponse<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse response = userService.getUserByEmail(email);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "사용자 정보 수정",
            description = "사용자의 이름, 전화번호 등을 수정합니다. 본인 또는 ADMIN/SUPER_ADMIN."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or #userId == principal.userId")
    @PatchMapping("/{userId}")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateUser(userId, request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "사용자 상태 변경",
            description = "사용자의 상태를 변경합니다 (ACTIVE/INACTIVE/SUSPENDED). ADMIN/SUPER_ADMIN 전용."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PatchMapping("/{userId}/status")
    public ApiResponse<UserResponse> updateUserStatus(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long userId,
            @RequestParam UserStatus status) {
        UserResponse response = userService.updateUserStatus(userId, status, currentUser.getUser());
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "이메일 중복 확인",
            description = "이메일 중복 여부를 확인합니다. (회원가입 플로우용, 비인증). " +
                    "남용 방지를 위해 Rate Limiting 적용 예정(P1-3)."
    )
    @GetMapping("/check-email")
    public ApiResponse<Boolean> checkEmailDuplicate(@RequestParam String email) {
        boolean exists = userService.checkEmailExists(email);
        return ApiResponse.success(exists);
    }

    @Operation(
            summary = "사용자 검색",
            description = "키워드, 역할, 상태로 사용자를 검색합니다 (페이징). ADMIN/SUPER_ADMIN 전용."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/search")
    public ApiResponse<PageResponse<UserResponse>> searchUsers(UserSearchCondition condition) {
        PageResponse<UserResponse> response = userService.searchUsers(condition);
        return ApiResponse.success(response);
    }
}
