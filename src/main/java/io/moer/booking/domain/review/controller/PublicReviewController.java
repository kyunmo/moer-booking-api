package io.moer.booking.domain.review.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 고객용 리뷰 Public API
 * 인증 없이 접근 가능
 */
@RestController
@RequestMapping("/api/public/businesses/{slug}/reviews")
@RequiredArgsConstructor
@Tag(name = "Public Review", description = "고객용 리뷰 API (비인증)")
public class PublicReviewController {

    private final ReviewService reviewService;
    private final BusinessRepository businessRepository;

    /**
     * 리뷰 목록 조회 (Public)
     * ACTIVE 상태만, 고객명 마스킹 처리
     */
    @GetMapping
    @Operation(
            summary = "리뷰 목록 조회",
            description = "매장의 리뷰 목록을 조회합니다. ACTIVE 상태만 노출되며 고객명은 마스킹됩니다."
    )
    public ApiResponse<Map<String, Object>> getReviews(
            @Parameter(description = "매장 슬러그", required = true) @PathVariable String slug,
            @Parameter(description = "페이지 번호 (1부터)") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(required = false, defaultValue = "10") Integer size,
            @Parameter(description = "별점 필터 (1~5)") @RequestParam(required = false) Integer rating,
            @Parameter(description = "스태프 ID 필터") @RequestParam(required = false) Long staffId,
            @Parameter(description = "정렬 기준 (latest, rating_high, rating_low)") @RequestParam(required = false, defaultValue = "latest") String sortBy) {

        Long businessId = resolveBusinessId(slug);
        Map<String, Object> response = reviewService.getPublicReviews(businessId, rating, staffId, sortBy, page, size);
        return ApiResponse.success(response);
    }

    /**
     * slug -> businessId 변환
     */
    private Long resolveBusinessId(String slug) {
        Business business = businessRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND,
                        "매장을 찾을 수 없습니다: " + slug));
        return business.getId();
    }
}
