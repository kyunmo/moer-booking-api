package io.moer.booking.domain.review.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.domain.review.dto.ReviewDeleteRequest;
import io.moer.booking.domain.review.dto.ReviewReplyRequest;
import io.moer.booking.domain.review.dto.ReviewSearchCondition;
import io.moer.booking.domain.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 리뷰 관리 API (Admin)
 * 인증 필요, 매장 관리자용
 */
@RestController
@RequestMapping("/api/businesses/{businessId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Review Admin", description = "리뷰 관리 API (Admin)")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 관리자 리뷰 목록 조회
     * 전체 상태 조회, 통계 정보 포함
     */
    @GetMapping
    @Operation(
            summary = "관리자 리뷰 목록 조회",
            description = "매장의 리뷰를 관리자 권한으로 조회합니다. 상태 필터, 별점 필터, 기간 필터를 지원합니다."
    )
    public ApiResponse<Map<String, Object>> getAdminReviews(
            @Parameter(description = "매장 ID", required = true) @PathVariable Long businessId,
            @Parameter(description = "페이지 번호 (1부터)") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(required = false, defaultValue = "10") Integer size,
            @Parameter(description = "상태 필터 (ACTIVE, HIDDEN, DELETED)") @RequestParam(required = false) String status,
            @Parameter(description = "별점 필터 (1~5)") @RequestParam(required = false) Integer rating,
            @Parameter(description = "스태프 ID 필터") @RequestParam(required = false) Long staffId,
            @Parameter(description = "검색 시작일") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "검색 종료일") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        ReviewSearchCondition condition = ReviewSearchCondition.builder()
                .businessId(businessId)
                .status(status)
                .rating(rating)
                .staffId(staffId)
                .startDate(startDate)
                .endDate(endDate)
                .page(page)
                .size(size)
                .build();

        Map<String, Object> response = reviewService.getAdminReviews(businessId, condition);
        return ApiResponse.success(response);
    }

    /**
     * 리뷰 답변 등록
     */
    @PostMapping("/{reviewId}/reply")
    @Operation(
            summary = "리뷰 답변 등록",
            description = "리뷰에 사장님 답변을 등록합니다. 이미 답변이 등록된 리뷰에는 중복 등록할 수 없습니다."
    )
    public ApiResponse<Void> replyToReview(
            @Parameter(description = "매장 ID", required = true) @PathVariable Long businessId,
            @Parameter(description = "리뷰 ID", required = true) @PathVariable Long reviewId,
            @Valid @RequestBody ReviewReplyRequest request) {

        reviewService.replyToReview(businessId, reviewId, request);
        return ApiResponse.success();
    }

    /**
     * 리뷰 삭제 (소프트 삭제)
     */
    @DeleteMapping("/{reviewId}")
    @Operation(
            summary = "리뷰 삭제",
            description = "리뷰를 소프트 삭제합니다. 삭제 사유를 선택적으로 입력할 수 있습니다."
    )
    public ApiResponse<Void> deleteReview(
            @Parameter(description = "매장 ID", required = true) @PathVariable Long businessId,
            @Parameter(description = "리뷰 ID", required = true) @PathVariable Long reviewId,
            @RequestBody(required = false) ReviewDeleteRequest request) {

        reviewService.deleteReview(businessId, reviewId, request);
        return ApiResponse.success();
    }
}
