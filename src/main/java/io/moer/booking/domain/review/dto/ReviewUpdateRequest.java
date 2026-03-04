package io.moer.booking.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "리뷰 수정 요청")
public class ReviewUpdateRequest {

    @Min(value = 1, message = "별점은 1~5 사이여야 합니다")
    @Max(value = 5, message = "별점은 1~5 사이여야 합니다")
    @Schema(description = "별점 (1~5)", example = "5")
    private Integer rating;

    @Size(max = 2000, message = "리뷰 내용은 2000자 이내여야 합니다")
    @Schema(description = "리뷰 내용")
    private String content;
}
