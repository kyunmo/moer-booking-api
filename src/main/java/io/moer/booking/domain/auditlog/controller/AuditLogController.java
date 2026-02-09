package io.moer.booking.domain.auditlog.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.auditlog.dto.AuditLogResponse;
import io.moer.booking.domain.auditlog.dto.AuditLogSearchCondition;
import io.moer.booking.domain.auditlog.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 감사 로그 조회 API
 * SUPER_ADMIN만 접근 가능
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 감사 로그 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getLog(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {

        if (!currentUser.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
        }

        AuditLogResponse response = auditLogService.getLog(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 감사 로그 목록 조회 (검색, 페이징)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getLogs(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @ModelAttribute AuditLogSearchCondition condition,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (!currentUser.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
        }

        PageResponse<AuditLogResponse> response = auditLogService.getLogs(condition, page, size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
