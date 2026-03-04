package io.moer.booking.domain.notification.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.notification.dto.NotificationSendRequest;
import io.moer.booking.domain.notification.dto.NotificationSendResponse;
import io.moer.booking.domain.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "매장 알림 발송", description = "관리자 고객 알림 발송 API")
@RestController
@RequestMapping("/api/businesses/{businessId}/notifications")
@RequiredArgsConstructor
public class BusinessNotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    @Operation(summary = "고객 알림 발송", description = "매장의 고객들에게 알림을 발송합니다. 전체/특정/세그먼트 대상 선택 가능.")
    public ResponseEntity<ApiResponse<NotificationSendResponse>> sendNotification(
            @PathVariable Long businessId,
            @Valid @RequestBody NotificationSendRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (!userDetails.getUser().canAccessBusiness(businessId)) {
            throw new BusinessException(ErrorCode.BUSINESS_ACCESS_DENIED);
        }

        NotificationSendResponse response = notificationService.sendToCustomers(
                businessId, request, userDetails.getUser().getId());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }
}
