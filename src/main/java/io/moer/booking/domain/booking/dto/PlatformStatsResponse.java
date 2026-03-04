package io.moer.booking.domain.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "플랫폼 통계 응답 (랜딩 페이지용)")
public class PlatformStatsResponse {

    @Schema(description = "총 매장 수 (ACTIVE)", example = "128")
    private long totalBusinesses;

    @Schema(description = "총 예약 수", example = "5420")
    private long totalReservations;

    @Schema(description = "총 리뷰 수 (ACTIVE)", example = "1230")
    private long totalReviews;

    @Schema(description = "전체 평균 평점 (소수점 1자리)", example = "4.5")
    private double avgRating;

    @Schema(description = "활성 구독 매장 수 (ACTIVE + TRIAL)", example = "95")
    private long activePlansCount;

    @Schema(description = "통계 조회 시간")
    private LocalDateTime updatedAt;
}
