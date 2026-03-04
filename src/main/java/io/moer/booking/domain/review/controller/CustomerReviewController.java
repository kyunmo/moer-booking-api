package io.moer.booking.domain.review.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.review.dto.CustomerReviewCreateRequest;
import io.moer.booking.domain.review.dto.ReviewImageResponse;
import io.moer.booking.domain.review.dto.ReviewResponse;
import io.moer.booking.domain.review.dto.ReviewUpdateRequest;
import io.moer.booking.domain.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 로그인 고객용 리뷰 API
 * JWT 인증 필요 (CUSTOMER 역할)
 *
 * - 내 리뷰 목록: GET /api/customer/reviews
 * - 리뷰 작성: POST /api/customer/businesses/{slug}/reviews
 * - 리뷰 수정: PUT /api/customer/reviews/{reviewId}
 * - 리뷰 삭제: DELETE /api/customer/reviews/{reviewId}
 * - 이미지 업로드: POST /api/customer/reviews/{reviewId}/images
 * - 이미지 삭제: DELETE /api/customer/reviews/{reviewId}/images/{imageId}
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Customer Review", description = "로그인 고객용 리뷰 API (인증 필요)")
public class CustomerReviewController {

    private final ReviewService reviewService;
    private final io.moer.booking.common.storage.FileStorageService fileStorageService;

    // ========================================
    // 0. 내 리뷰 목록 조회
    // ========================================

    /**
     * 내 리뷰 목록 조회
     * - businessName, businessSlug 포함
     */
    @GetMapping("/api/customer/reviews")
    @Operation(
            summary = "내 리뷰 목록 조회",
            description = "로그인 고객이 작성한 리뷰 목록을 조회합니다. businessName, businessSlug가 포함됩니다."
    )
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getMyReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "페이지 번호 (1부터)", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지당 개수", example = "10") @RequestParam(defaultValue = "10") int size) {

        PageResponse<ReviewResponse> response = reviewService.getMyReviews(userDetails.getUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ========================================
    // 1. 리뷰 작성
    // ========================================

    /**
     * 로그인 고객 리뷰 작성
     * - JWT 인증된 사용자의 userId로 본인 예약 확인
     * - 전화번호 검증 불필요
     */
    @PostMapping("/api/customer/businesses/{slug}/reviews")
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

    // ========================================
    // 1-2. 리뷰 작성 (Multipart - 이미지 동시 업로드)
    // ========================================

    /**
     * 로그인 고객 리뷰 작성 (이미지 동시 업로드)
     * - multipart/form-data로 리뷰 데이터 + 이미지를 한번에 전송
     * - 기존 JSON 엔드포인트와 consumes로 구분
     */
    @PostMapping(value = "/api/customer/businesses/{slug}/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "로그인 고객 리뷰 작성 (이미지 동시 업로드)",
            description = "JWT 인증된 고객이 리뷰를 작성하면서 이미지를 동시에 업로드합니다. multipart/form-data로 전송합니다."
    )
    public ResponseEntity<ApiResponse<ReviewResponse>> createReviewWithImages(
            @Parameter(description = "매장 슬러그", required = true) @PathVariable String slug,
            @RequestParam("rating") Integer rating,
            @RequestParam("content") String content,
            @RequestParam("reservationNumber") String reservationNumber,
            @RequestParam(value = "staffId", required = false) Long staffId,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CustomerReviewCreateRequest request = new CustomerReviewCreateRequest(
                reservationNumber, rating, content, staffId);

        ReviewResponse response = reviewService.createReviewWithImages(
                slug, userDetails.getUserId(), request, images);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    // ========================================
    // 2. 리뷰 수정
    // ========================================

    /**
     * 리뷰 수정 (본인 확인)
     * - 별점, 내용 부분 수정 가능
     * - 별점 변경 시 매장 통계 자동 갱신
     */
    @PutMapping("/api/customer/reviews/{reviewId}")
    @Operation(
            summary = "리뷰 수정",
            description = "본인 리뷰의 별점/내용을 수정합니다. null인 필드는 기존 값이 유지됩니다."
    )
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "리뷰 ID", required = true) @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request) {

        ReviewResponse response = reviewService.updateReview(reviewId, userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ========================================
    // 3. 리뷰 삭제
    // ========================================

    /**
     * 리뷰 삭제 (본인 확인, 소프트 삭제)
     * - 상태를 DELETED로 변경
     * - 매장 평점 통계 재계산
     */
    @DeleteMapping("/api/customer/reviews/{reviewId}")
    @Operation(
            summary = "리뷰 삭제",
            description = "본인 리뷰를 삭제합니다. 소프트 삭제로 처리되며 매장 평점 통계가 재계산됩니다."
    )
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "리뷰 ID", required = true) @PathVariable Long reviewId) {

        reviewService.deleteReviewByCustomer(reviewId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    // ========================================
    // 4. 리뷰 이미지 업로드
    // ========================================

    /**
     * 리뷰 이미지 업로드
     * - 리뷰당 최대 5개 이미지
     * - 본인 리뷰만 가능
     */
    @PostMapping("/api/customer/reviews/{reviewId}/images")
    @Operation(
            summary = "리뷰 이미지 업로드",
            description = "리뷰에 이미지를 첨부합니다. 리뷰당 최대 5개까지 등록 가능합니다."
    )
    public ResponseEntity<ApiResponse<ReviewImageResponse>> uploadImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "리뷰 ID", required = true) @PathVariable Long reviewId,
            @RequestParam("file") MultipartFile file) {

        // FileStorageService로 파일 저장
        String imageUrl = fileStorageService.store(file, "reviews/" + reviewId);
        String thumbnailUrl = imageUrl; // TODO: 실제 구현에서는 리사이즈 후 별도 URL 생성

        ReviewImageResponse response = reviewService.addReviewImage(
                reviewId, userDetails.getUserId(),
                imageUrl, thumbnailUrl,
                file.getOriginalFilename(), (int) file.getSize());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    // ========================================
    // 5. 리뷰 이미지 삭제
    // ========================================

    /**
     * 리뷰 이미지 삭제
     * - 본인 리뷰의 이미지만 삭제 가능
     */
    @DeleteMapping("/api/customer/reviews/{reviewId}/images/{imageId}")
    @Operation(
            summary = "리뷰 이미지 삭제",
            description = "리뷰의 특정 이미지를 삭제합니다. 본인 리뷰의 이미지만 삭제 가능합니다."
    )
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "리뷰 ID", required = true) @PathVariable Long reviewId,
            @Parameter(description = "이미지 ID", required = true) @PathVariable Long imageId) {

        reviewService.deleteReviewImage(reviewId, imageId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
