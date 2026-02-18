package io.moer.booking.domain.business.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.auth.dto.ProfileImageResponse;
import io.moer.booking.domain.business.BusinessStatus;
import io.moer.booking.domain.business.dto.*;
import io.moer.booking.domain.business.service.BusinessService;
import io.moer.booking.domain.business.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 매장 관리 Controller
 */
@RestController
@RequestMapping("/api/businesses")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;
    private final OnboardingService onboardingService;

    /**
     * 매장 생성
     */
    @PostMapping
    public ApiResponse<BusinessResponse> createBusiness(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody BusinessCreateRequest request) {
        BusinessResponse response = businessService.createBusiness(request, currentUser.getUser());
        return ApiResponse.success(response);
    }

    /**
     * 매장 단건 조회
     */
    @GetMapping("/{id}")
    public ApiResponse<BusinessResponse> getBusiness(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {
        BusinessResponse response = businessService.getBusiness(id, currentUser.getUser());
        return ApiResponse.success(response);
    }

    /**
     * 매장 목록 조회
     */
    @GetMapping
    public ApiResponse<PageResponse<BusinessResponse>> getBusinesses(BusinessSearchCondition condition) {
        PageResponse<BusinessResponse> response = businessService.getBusinesses(condition);
        return ApiResponse.success(response);
    }

    /**
     * Owner의 매장 목록 조회
     */
    @GetMapping("/owner/{ownerId}")
    public ApiResponse<List<BusinessResponse>> getBusinessesByOwner(@PathVariable Long ownerId) {
        List<BusinessResponse> response = businessService.getBusinessesByOwner(ownerId);
        return ApiResponse.success(response);
    }

    /**
     * 매장 수정
     */
    @PatchMapping("/{id}")
    public ApiResponse<BusinessResponse> updateBusiness(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody BusinessUpdateRequest request) {
        BusinessResponse response = businessService.updateBusiness(id, request, currentUser.getUser());
        return ApiResponse.success(response);
    }

    /**
     * 매장 Settings 수정
     */
    @PatchMapping("/{id}/settings")
    public ApiResponse<BusinessResponse> updateBusinessSettings(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @RequestBody BusinessSettingsUpdateRequest request) {
        BusinessResponse response = businessService.updateBusinessSettings(id, request, currentUser.getUser());
        return ApiResponse.success(response);
    }

    /**
     * 매장 삭제
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBusiness(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {
        businessService.deleteBusiness(id, currentUser.getUser());
        return ApiResponse.success();
    }

    /**
     * 매장 상태 변경
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<BusinessResponse> changeBusinessStatus(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @RequestParam BusinessStatus status) {
        BusinessResponse response = businessService.changeBusinessStatus(id, status, currentUser.getUser());
        return ApiResponse.success(response);
    }

    /**
     * 매장 프로필 이미지 업로드
     */
    @PostMapping(value = "/{id}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProfileImageResponse> uploadProfileImage(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @RequestParam("image") MultipartFile file) {
        ProfileImageResponse response = businessService.uploadProfileImage(id, file, currentUser.getUser());
        return ApiResponse.success(response);
    }

    /**
     * 매장 프로필 이미지 삭제
     */
    @DeleteMapping("/{id}/profile-image")
    public ApiResponse<Void> deleteProfileImage(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {
        businessService.deleteProfileImage(id, currentUser.getUser());
        return ApiResponse.success();
    }

    /**
     * 온보딩 상태 조회
     */
    @GetMapping("/{id}/onboarding")
    public ApiResponse<OnboardingStatusResponse> getOnboardingStatus(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {
        currentUser.getUser().canAccessBusiness(id);
        return ApiResponse.success(onboardingService.getOnboardingStatus(id));
    }

    /**
     * 온보딩 건너뛰기
     */
    @PostMapping("/{id}/onboarding/skip")
    public ApiResponse<Void> skipOnboarding(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {
        currentUser.getUser().canAccessBusiness(id);
        onboardingService.skipOnboarding(id);
        return ApiResponse.success(null, "온보딩을 건너뛰었습니다.");
    }
}