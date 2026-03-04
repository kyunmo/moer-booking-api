package io.moer.booking.domain.subscription.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.subscription.dto.PlanChangeRequest;
import io.moer.booking.domain.subscription.dto.SubscriptionInfoResponse;
import io.moer.booking.domain.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
     * - businessId 파라미터 없음: 현재 로그인 사용자의 매장 구독 조회
     * - businessId 파라미터 있음: 슈퍼관리자 전용, 특정 매장 구독 조회
     */
    @GetMapping
    @Operation(summary = "구독 정보 조회", description = "현재 매장의 구독 플랜 및 사용량 정보를 조회합니다. 슈퍼관리자는 businessId 파라미터로 특정 매장을 조회할 수 있습니다.")
    public ResponseEntity<ApiResponse<SubscriptionInfoResponse>> getSubscriptionInfo(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "매장 ID (슈퍼관리자 전용)")
            @RequestParam(required = false) Long businessId
    ) {
        Long targetBusinessId;

        if (businessId != null) {
            // businessId 파라미터가 있으면 슈퍼관리자 권한 체크
            if (!currentUser.isSuperAdmin()) {
                throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
            }
            targetBusinessId = businessId;
        } else {
            targetBusinessId = currentUser.getUser().getBusinessId();
        }

        SubscriptionInfoResponse response = subscriptionService.getSubscriptionInfo(targetBusinessId);
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
                request.getNewPlan(),
                request.getBillingCycle()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 구독 취소
     * - 취소 후 구독 정보를 반환합니다 (잔여 기간 안내용 nextBillingDate 포함)
     * - nextBillingDate: 유료 기능 사용 가능 마감일 (FE에서 expiresAt으로 활용)
     */
    @PostMapping("/cancel")
    @Operation(summary = "구독 취소", description = "현재 구독을 취소합니다. 잔여 기간 정보가 포함된 구독 정보를 반환합니다.")
    public ResponseEntity<ApiResponse<SubscriptionInfoResponse>> cancelSubscription(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Long businessId = currentUser.getUser().getBusinessId();
        SubscriptionInfoResponse response = subscriptionService.cancelSubscription(businessId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
