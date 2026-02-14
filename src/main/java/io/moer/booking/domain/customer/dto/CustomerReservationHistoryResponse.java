package io.moer.booking.domain.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 고객 예약 이력 응답 (목록 + 요약 통계)
 */
@Getter
@Builder
@Schema(description = "고객 예약 이력 응답")
public class CustomerReservationHistoryResponse {

    @Schema(description = "예약 목록")
    private List<CustomerReservationItem> items;

    @Schema(description = "전체 건수", example = "24")
    private int totalCount;

    @Schema(description = "요약 통계")
    private CustomerReservationSummary summary;
}
