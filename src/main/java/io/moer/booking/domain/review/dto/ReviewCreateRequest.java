package io.moer.booking.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "리뷰 작성 요청")
public class ReviewCreateRequest {

    @NotBlank(message = "예약번호는 필수입니다")
    @Schema(description = "예약번호", example = "RV20260214-001")
    private String reservationNumber;

    @NotBlank(message = "전화번호는 필수입니다")
    @Schema(description = "본인 확인용 전화번호", example = "010-1234-5678")
    private String phone;

    @NotNull(message = "별점은 필수입니다")
    @Min(value = 1, message = "별점은 1점 이상이어야 합니다")
    @Max(value = 5, message = "별점은 5점 이하여야 합니다")
    @Schema(description = "별점 (1~5)", example = "5")
    private Integer rating;

    @Schema(description = "리뷰 내용", example = "정말 좋은 서비스였습니다!")
    private String content;

    @Schema(description = "담당 스태프 ID (선택)", example = "1")
    private Long staffId;
}
