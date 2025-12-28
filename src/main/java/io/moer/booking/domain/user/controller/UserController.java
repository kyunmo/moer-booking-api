package io.moer.booking.domain.user.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.domain.user.UserStatus;
import io.moer.booking.domain.user.dto.*;
import io.moer.booking.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 회원 생성
     */
    @PostMapping
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return ApiResponse.success(response);
    }

    /**
     * 회원 단건 조회
     */
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        UserResponse response = userService.getUser(id);
        return ApiResponse.success(response);
    }

    /**
     * 회원 목록 조회
     */
    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> getUsers(UserSearchCondition condition) {
        PageResponse<UserResponse> response = userService.getUsers(condition);
        return ApiResponse.success(response);
    }

    /**
     * 회원 수정
     */
    @PatchMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateUser(id, request);
        return ApiResponse.success(response);
    }

    /**
     * 회원 삭제
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.success();
    }

    /**
     * 회원 상태 변경
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<UserResponse> changeUserStatus(
            @PathVariable Long id,
            @RequestParam UserStatus status) {
        UserResponse response = userService.changeUserStatus(id, status);
        return ApiResponse.success(response);
    }
}