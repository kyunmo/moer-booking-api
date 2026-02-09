package io.moer.booking.domain.superadmin.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.business.dto.BusinessResponse;
import io.moer.booking.domain.business.dto.BusinessSearchCondition;
import io.moer.booking.domain.superadmin.dto.BulkStatusUpdateRequest;
import io.moer.booking.domain.superadmin.service.SuperAdminBusinessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 슈퍼 관리자 - 매장 관리 API
 */
@RestController
@RequestMapping("/api/superadmin/businesses")
@RequiredArgsConstructor
public class SuperAdminBusinessController {

    private final SuperAdminBusinessService superAdminBusinessService;

    /**
     * 전체 매장 조회 (페이징, 필터링)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BusinessResponse>>> getAllBusinesses(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @ModelAttribute BusinessSearchCondition condition,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (!currentUser.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
        }

        PageResponse<BusinessResponse> response =
                superAdminBusinessService.getAllBusinesses(condition, page, size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 매장 강제 삭제 (소프트 or 하드)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBusiness(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean hard) {

        if (!currentUser.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
        }

        superAdminBusinessService.forceDeleteBusiness(id, hard, currentUser.getUser());

        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 매장 상태 일괄 변경
     */
    @PatchMapping("/bulk-status")
    public ResponseEntity<ApiResponse<Void>> bulkUpdateStatus(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody BulkStatusUpdateRequest request) {

        if (!currentUser.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
        }

        superAdminBusinessService.bulkUpdateStatus(request, currentUser.getUser());

        return ResponseEntity.ok(ApiResponse.success());
    }
}
