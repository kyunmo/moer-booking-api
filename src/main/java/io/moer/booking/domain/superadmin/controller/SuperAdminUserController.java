package io.moer.booking.domain.superadmin.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.superadmin.dto.ChangeRoleRequest;
import io.moer.booking.domain.superadmin.service.SuperAdminUserService;
import io.moer.booking.domain.user.dto.UserResponse;
import io.moer.booking.domain.user.dto.UserSearchCondition;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 슈퍼 관리자 - 사용자 관리 API
 */
@RestController
@RequestMapping("/api/superadmin/users")
@RequiredArgsConstructor
public class SuperAdminUserController {

    private final SuperAdminUserService superAdminUserService;

    /**
     * 전체 사용자 조회 (페이징, 필터링)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @ModelAttribute UserSearchCondition condition,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (!currentUser.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
        }

        PageResponse<UserResponse> response =
                superAdminUserService.getAllUsers(condition, page, size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 역할 변경
     */
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequest request) {

        if (!currentUser.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
        }

        UserResponse response = superAdminUserService.changeUserRole(
                id, request.getRole(), currentUser.getUser());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 강제 정지
     */
    @PatchMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<UserResponse>> suspendUser(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {

        if (!currentUser.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
        }

        UserResponse response = superAdminUserService.suspendUser(id, currentUser.getUser());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 강제 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {

        if (!currentUser.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
        }

        superAdminUserService.forceDeleteUser(id, currentUser.getUser());

        return ResponseEntity.ok(ApiResponse.success());
    }
}
