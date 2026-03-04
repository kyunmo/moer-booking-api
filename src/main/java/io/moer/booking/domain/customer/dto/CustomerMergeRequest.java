package io.moer.booking.domain.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "고객 병합 요청")
public class CustomerMergeRequest {

    @NotNull(message = "주 고객 ID는 필수입니다")
    @Schema(description = "주 고객 ID (유지할 고객)", example = "1")
    private Long primaryCustomerId;

    @NotNull(message = "병합할 고객 ID 목록은 필수입니다")
    @Size(min = 1, message = "병합할 고객이 최소 1명 이상 있어야 합니다")
    @Schema(description = "병합할 고객 ID 목록 (삭제될 고객)", example = "[2, 3]")
    private List<Long> mergeCustomerIds;

    @Schema(description = "병합 전략 (KEEP_PRIMARY: 주 고객 정보 유지)", example = "KEEP_PRIMARY")
    private String strategy;
}
