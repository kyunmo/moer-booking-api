package io.moer.booking.domain.user.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.domain.user.UserStatus;
import io.moer.booking.domain.user.dto.UserCreateRequest;
import io.moer.booking.domain.user.dto.UserResponse;
import io.moer.booking.domain.user.dto.UserSearchCondition;
import io.moer.booking.domain.user.dto.UserUpdateRequest;
import io.moer.booking.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "사용자", description = "사용자 관리 API (회원가입, 조회, 수정, 검색)")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "회원가입",
            description = "새로운 사용자를 등록합니다. (인증 불필요)"
    )
    @PostMapping
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "사용자 목록 조회",
            description = "전체 사용자 목록을 조회합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ApiResponse<List<UserResponse>> getUsers() {
        List<UserResponse> response = userService.getAllUsers();
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "사용자 단건 조회",
            description = "ID로 사용자를 조회합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long userId) {
        UserResponse response = userService.getUser(userId);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "이메일로 사용자 조회",
            description = "이메일로 사용자를 조회합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/email/{email}")
    public ApiResponse<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse response = userService.getUserByEmail(email);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "사용자 정보 수정",
            description = "사용자의 이름, 전화번호 등을 수정합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{userId}")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateUser(userId, request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "사용자 상태 변경",
            description = "사용자의 상태(ACTIVE, INACTIVE, SUSPENDED)를 변경합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{userId}/status")
    public ApiResponse<UserResponse> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam UserStatus status) {
        UserResponse response = userService.updateUserStatus(userId, status);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "이메일 중복 확인",
            description = "이메일이 이미 사용 중인지 확인합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/check-email")
    public ApiResponse<Boolean> checkEmailExists(@RequestParam String email) {
        boolean exists = userService.checkEmailExists(email);
        return ApiResponse.success(exists);
    }

    @Operation(
            summary = "사용자 검색 (페이징)",
            description = "키워드, 역할, 상태 등으로 사용자를 검색합니다. 페이징을 지원합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/search")
    public ApiResponse<PageResponse<UserResponse>> searchUsers(
            @ModelAttribute UserSearchCondition condition) {
        PageResponse<UserResponse> response = userService.searchUsers(condition);
        return ApiResponse.success(response);
    }
}