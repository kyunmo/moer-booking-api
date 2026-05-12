package io.moer.booking.domain.notificationlog.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.notificationlog.NotificationChannel;
import io.moer.booking.domain.notificationlog.NotificationLogStatus;
import io.moer.booking.domain.notificationlog.NotificationTemplateType;
import io.moer.booking.domain.notificationlog.dto.NotificationLogResponse;
import io.moer.booking.domain.notificationlog.service.NotificationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/businesses/{businessId}/notification-logs")
@RequiredArgsConstructor
@Tag(name = "NotificationLog", description = "알림 발송 이력 조회 API")
public class NotificationLogController {

    private final NotificationLogService notificationLogService;

    @Operation(
            summary = "알림 발송 이력 목록 조회",
            description = "매장의 외부 알림(카카오/SMS/이메일) 발송 이력을 페이징 조회합니다."
    )
    @GetMapping
    public ApiResponse<PageResponse<NotificationLogResponse>> getNotificationLogs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @Parameter(description = "발송 채널 (KAKAO, SMS, EMAIL)")
            @RequestParam(required = false) NotificationChannel channel,
            @Parameter(description = "발송 상태 (PENDING, SENT, FAILED)")
            @RequestParam(required = false) NotificationLogStatus status,
            @Parameter(description = "템플릿 타입 (RESERVATION_CREATED 등)")
            @RequestParam(required = false) NotificationTemplateType templateType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);

        PageResponse<NotificationLogResponse> response = notificationLogService
                .getNotificationLogs(businessId, channel, status, templateType, page, size);

        return ApiResponse.success(response);
    }

    @Operation(
            summary = "알림 발송 이력 단건 조회",
            description = "특정 알림 발송 이력의 상세 정보를 조회합니다."
    )
    @GetMapping("/{id}")
    public ApiResponse<NotificationLogResponse> getNotificationLog(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId,
            @PathVariable Long id) {

        // SECURITY (P1-4): enforce — 위반 시 AccessDeniedException
        userDetails.getUser().requireAccessBusiness(businessId);

        NotificationLogResponse response = notificationLogService.getNotificationLog(id);

        return ApiResponse.success(response);
    }
}
