package io.moer.booking.domain.booking.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.booking.dto.SlugUpdateRequest;
import io.moer.booking.domain.booking.service.PublicBusinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 매장 슬러그 관리 Controller
 * 매장 관리자(Admin)만 접근 가능 (인증 필요)
 */
@RestController
@RequestMapping("/api/businesses/{businessId}")
@RequiredArgsConstructor
@Tag(name = "Business Slug", description = "매장 슬러그 관리 API (인증 필요)")
public class BusinessSlugController {

    private final PublicBusinessService publicBusinessService;

    /**
     * 슬러그 변경
     */
    @PatchMapping("/slug")
    @Operation(
            summary = "매장 슬러그 변경",
            description = "매장의 슬러그를 변경합니다. 매장 관리자만 접근 가능."
    )
    public ApiResponse<Void> updateSlug(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long businessId,
            @Valid @RequestBody SlugUpdateRequest request) {

        // 권한 체크
        if (!currentUser.getUser().canAccessBusiness(businessId)) {
            throw new BusinessException(ErrorCode.BUSINESS_ACCESS_DENIED);
        }

        publicBusinessService.updateSlug(businessId, request.getSlug());

        return ApiResponse.success(null, "슬러그가 변경되었습니다.");
    }
}
