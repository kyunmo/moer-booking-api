package io.moer.booking.domain.notification.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.notification.dto.NotificationListResponse;
import io.moer.booking.domain.notification.service.NotificationService;
import io.moer.booking.domain.notification.service.SseEmitterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "알림", description = "알림 조회 및 읽음 처리 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    @Operation(
            summary = "SSE 알림 스트림",
            description = "실시간 알림을 받기 위한 SSE 스트림에 연결합니다. 30분 타임아웃."
    )
    @GetMapping(value = "/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return sseEmitterService.subscribe(userDetails.getUserId());
    }

    @Operation(
            summary = "수신 공지 조회",
            description = "매장에 수신된 전체 공지를 조회합니다."
    )
    @GetMapping("/broadcasts")
    public ApiResponse<NotificationListResponse> getBroadcastNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        // broadcasts are stored as SYSTEM type notifications with referenceType=BROADCAST
        NotificationListResponse response = notificationService.getNotifications(
                userDetails.getUserId(), page, size, false);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "알림 목록 조회",
            description = "현재 사용자의 알림 목록을 조회합니다."
    )
    @GetMapping
    public ApiResponse<NotificationListResponse> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        NotificationListResponse response = notificationService.getNotifications(
                userDetails.getUserId(), page, size, unreadOnly);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "알림 읽음 처리",
            description = "특정 알림을 읽음으로 표시합니다."
    )
    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        notificationService.markAsRead(userDetails.getUserId(), id);
        return ApiResponse.success(null, "읽음 처리되었습니다.");
    }

    @Operation(
            summary = "전체 알림 읽음 처리",
            description = "모든 알림을 읽음으로 표시합니다."
    )
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getUserId());
        return ApiResponse.success(null, "모든 알림을 읽음 처리했습니다.");
    }
}
