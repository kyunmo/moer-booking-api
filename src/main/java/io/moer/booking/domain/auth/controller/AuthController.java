package io.moer.booking.domain.auth.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.auth.dto.*;
import io.moer.booking.domain.auth.service.AuthService;
import io.moer.booking.domain.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "인증", description = "로그인, 로그아웃, 토큰 갱신, 회원가입 API")  // 👈 수정
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

    @Operation(
            summary = "회원가입",
            description = "사용자와 매장을 동시에 생성하고 JWT 토큰을 발급받습니다."
    )
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "비밀번호 찾기",
            description = "이메일로 비밀번호 재설정 링크를 발송합니다."
    )
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.getEmail());
        return ApiResponse.success(
                null,
                "비밀번호 재설정 이메일을 발송했습니다. 이메일을 확인해주세요."
        );
    }

    @Operation(
            summary = "비밀번호 재설정",
            description = "토큰을 사용하여 새 비밀번호로 변경합니다."
    )
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ApiResponse.success(
                null,
                "비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해주세요."
        );
    }

    @Operation(
            summary = "비밀번호 변경",
            description = "현재 비밀번호를 확인하고 새 비밀번호로 변경합니다."
    )
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userDetails.getUserId(), request);
        return ApiResponse.success(null, "비밀번호가 변경되었습니다.");
    }

    @Operation(
            summary = "프로필 수정",
            description = "이름, 전화번호를 수정합니다. null인 필드는 변경하지 않습니다."
    )
    @PatchMapping("/profile")
    public ApiResponse<UserResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProfileUpdateRequest request) {
        UserResponse response = authService.updateProfile(userDetails.getUserId(), request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "프로필 이미지 업로드",
            description = "프로필 이미지를 업로드합니다. (최대 5MB, jpg/png/webp)"
    )
    @PostMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProfileImageResponse> uploadProfileImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        ProfileImageResponse response = authService.uploadProfileImage(userDetails.getUserId(), file);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "SNS 연결 계정 목록",
            description = "현재 사용자에게 연결된 SNS 계정 목록을 조회합니다."
    )
    @GetMapping("/social-accounts")
    public ApiResponse<List<SnsAccountResponse>> getSocialAccounts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<SnsAccountResponse> response = authService.getSocialAccounts(userDetails.getUserId());
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "SNS 계정 연결 해제",
            description = "특정 SNS 계정의 연결을 해제합니다."
    )
    @DeleteMapping("/social-accounts/{provider}")
    public ApiResponse<Void> disconnectSocialAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String provider) {
        authService.disconnectSocialAccount(userDetails.getUserId(), provider);
        return ApiResponse.success(null, provider.toUpperCase() + " 계정 연결이 해제되었습니다.");
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "비밀번호를 확인하고 회원 탈퇴를 진행합니다. (Soft Delete)"
    )
    @DeleteMapping("/account")
    public ApiResponse<Void> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DeleteAccountRequest request) {
        authService.deleteAccount(userDetails.getUserId(), request);
        return ApiResponse.success(null, "회원 탈퇴가 완료되었습니다.");
    }
}