package io.moer.booking.domain.subscription.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.subscription.dto.PlanChangeRequest;
import io.moer.booking.domain.subscription.dto.SubscriptionInfoResponse;
import io.moer.booking.domain.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
@Tag(name = "구독 관리", description = "구독 플랜 조회 및 변경 API")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * 구독 정보 조회
     */
    @GetMapping
    @Operation(summary = "구독 정보 조회", description = "현재 매장의 구독 플랜 및 사용량 정보를 조회합니다")
    public ResponseEntity<ApiResponse<SubscriptionInfoResponse>> getSubscriptionInfo(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Long businessId = currentUser.getUser().getBusinessId();
        SubscriptionInfoResponse response = subscriptionService.getSubscriptionInfo(businessId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 플랜 변경
     */
    @PostMapping("/change-plan")
    @Operation(summary = "플랜 변경", description = "구독 플랜을 변경합니다. 다운그레이드 시 현재 사용량을 체크합니다")
    public ResponseEntity<ApiResponse<SubscriptionInfoResponse>> changePlan(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody PlanChangeRequest request
    ) {
        Long businessId = currentUser.getUser().getBusinessId();
        SubscriptionInfoResponse response = subscriptionService.changePlan(
                businessId,
                request.getNewPlan()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 구독 취소
     */
    @PostMapping("/cancel")
    @Operation(summary = "구독 취소", description = "현재 구독을 취소합니다")
    public ResponseEntity<ApiResponse<Void>> cancelSubscription(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Long businessId = currentUser.getUser().getBusinessId();
        subscriptionService.cancelSubscription(businessId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
