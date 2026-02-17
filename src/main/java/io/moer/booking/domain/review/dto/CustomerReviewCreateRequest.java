package io.moer.booking.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 고객의 리뷰 작성 요청
 * - 전화번호 불필요 (JWT 인증으로 본인 확인)
 * - 예약번호 + userId로 본인 예약 검증
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그인 고객 리뷰 작성 요청")
public class CustomerReviewCreateRequest {

    @NotBlank(message = "예약번호는 필수입니다")
    @Schema(description = "예약번호", example = "RV20260214-001")
    private String reservationNumber;

    @NotNull(message = "별점은 필수입니다")
    @Min(value = 1, message = "별점은 1~5 사이여야 합니다")
    @Max(value = 5, message = "별점은 1~5 사이여야 합니다")
    @Schema(description = "별점 (1~5)", example = "5")
    private Integer rating;

    @Schema(description = "리뷰 내용", example = "정말 좋은 서비스였습니다!")
    private String content;

    @Schema(description = "담당 스태프 ID (선택, 미입력 시 예약의 스태프 사용)", example = "1")
    private Long staffId;
}
