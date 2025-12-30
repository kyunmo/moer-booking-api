package io.moer.booking.domain.auth.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.auth.dto.LoginRequest;
import io.moer.booking.domain.auth.dto.LoginResponse;
import io.moer.booking.domain.auth.dto.RefreshTokenRequest;
import io.moer.booking.domain.auth.service.AuthService;
import io.moer.booking.domain.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "로그인, 로그아웃, 토큰 갱신 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다."
    )
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "토큰 갱신",
            description = "Refresh Token으로 새로운 Access Token을 발급받습니다."
    )
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshAccessToken(request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "로그아웃",
            description = "Refresh Token을 삭제하여 로그아웃합니다."
    )
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.logout(userDetails.getUserId());
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "현재 로그인 사용자 정보",
            description = "현재 로그인된 사용자의 정보를 조회합니다."
    )
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponse response = UserResponse.from(userDetails.getUser());
        return ApiResponse.success(response);
    }
}