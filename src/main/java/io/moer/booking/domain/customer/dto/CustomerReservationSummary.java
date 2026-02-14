package io.moer.booking.domain.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 고객 예약 이력 - 요약 통계
 */
@Getter
@Builder
@Schema(description = "고객 예약 요약 통계")
public class CustomerReservationSummary {

    @Schema(description = "총 방문 횟수", example = "24")
    private Integer totalVisits;

    @Schema(description = "총 지출 금액", example = "1200000")
    private Integer totalSpent;

    @Schema(description = "마지막 방문일", example = "2026-02-10")
    private LocalDate lastVisitDate;

    @Schema(description = "가장 많이 이용한 서비스", example = "여성컷")
    private String favoriteService;

    @Schema(description = "가장 많이 만난 직원", example = "김디자이너")
    private String favoriteStaff;
}
