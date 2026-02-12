package io.moer.booking.domain.coupon.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.coupon.dto.CouponCreateRequest;
import io.moer.booking.domain.coupon.dto.CouponResponse;
import io.moer.booking.domain.coupon.dto.CouponSearchCondition;
import io.moer.booking.domain.coupon.service.CouponService;
import io.moer.booking.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 쿠폰 API
 */
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupon", description = "쿠폰 관리 API")
public class CouponController {

    private final CouponService couponService;

    /**
     * 쿠폰 생성
     */
    @PostMapping
    @Operation(summary = "쿠폰 생성", description = "새로운 쿠폰을 생성합니다")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CouponCreateRequest request
    ) {
        User user = userDetails.getUser();
        CouponResponse response = couponService.createCoupon(user.getBusinessId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
    }

    /**
     * 쿠폰 조회 (단건)
     */
    @GetMapping("/{couponId}")
    @Operation(summary = "쿠폰 조회", description = "쿠폰 상세 정보를 조회합니다")
    public ResponseEntity<ApiResponse<CouponResponse>> getCoupon(
            @PathVariable Long couponId
    ) {
        CouponResponse response = couponService.getCoupon(couponId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 쿠폰 검증 (사용 가능 여부)
     */
    @PostMapping("/validate")
    @Operation(summary = "쿠폰 검증", description = "쿠폰 사용 가능 여부를 검증하고 할인 금액을 계산합니다")
    public ResponseEntity<ApiResponse<CouponResponse>> validateCoupon(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, Object> body
    ) {
        User user = userDetails.getUser();
        String code = (String) body.get("code");
        int orderAmount = (int) body.get("orderAmount");

        CouponResponse response = couponService.validateCoupon(code, user.getId(), orderAmount);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 쿠폰 목록 조회
     */
    @GetMapping
    @Operation(summary = "쿠폰 목록 조회", description = "쿠폰 목록을 조회합니다")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getCouponList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = userDetails.getUser();
        CouponSearchCondition condition = CouponSearchCondition.builder()
            .businessId(user.getBusinessId())
            .keyword(keyword)
            .status(status != null ? io.moer.booking.domain.coupon.CouponStatus.valueOf(status) : null)
            .page(page)
            .size(size)
            .build();

        List<CouponResponse> coupons = couponService.getCouponList(condition);
        return ResponseEntity.ok(ApiResponse.success(coupons));
    }
}
