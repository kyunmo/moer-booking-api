package io.moer.booking.domain.review.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.review.dto.CustomerReviewCreateRequest;
import io.moer.booking.domain.review.dto.ReviewResponse;
import io.moer.booking.domain.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 로그인 고객용 리뷰 API
 * JWT 인증 필요 (CUSTOMER 역할)
 */
@RestController
@RequestMapping("/api/customer/businesses/{slug}/reviews")
@RequiredArgsConstructor
@Tag(name = "Customer Review", description = "로그인 고객용 리뷰 API (인증 필요)")
public class CustomerReviewController {

    private final ReviewService reviewService;

    /**
     * 로그인 고객 리뷰 작성
     * - JWT 인증된 사용자의 userId로 본인 예약 확인
     * - 전화번호 검증 불필요
     */
    @PostMapping
    @Operation(
            summary = "로그인 고객 리뷰 작성",
            description = "JWT 인증된 고객이 완료된 예약에 대해 리뷰를 작성합니다. 예약번호로 본인 예약을 확인합니다."
    )
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Parameter(description = "매장 슬러그", required = true) @PathVariable String slug,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CustomerReviewCreateRequest request) {

        ReviewResponse response = reviewService.createReviewByCustomer(
                slug, userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}
