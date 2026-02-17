package io.moer.booking.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Admin 리뷰 통계 응답
 */
@Getter
@Builder
@Schema(description = "리뷰 통계")
public class ReviewStatsResponse {

    @Schema(description = "평균 평점", example = "4.3")
    private Double averageRating;

    @Schema(description = "전체 리뷰 수", example = "128")
    private int totalReviews;

    @Schema(description = "미답변 리뷰 수", example = "5")
    private int unrepliedCount;

    @Schema(description = "이번 달 리뷰 수", example = "12")
    private int thisMonthCount;

    @Schema(description = "별점 분포 (1~5점 별 개수)", example = "{\"1\": 3, \"2\": 5, \"3\": 10, \"4\": 30, \"5\": 80}")
    private Map<Integer, Integer> ratingDistribution;
}
