package io.moer.booking.domain.service.category.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 정렬 순서 변경 요청 DTO (카테고리/서비스 공용)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SortOrderUpdateRequest {

    @NotEmpty(message = "정렬 항목은 최소 1개 이상이어야 합니다")
    @Valid
    private List<SortOrderItem> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SortOrderItem {

        @NotNull(message = "ID는 필수입니다")
        private Long id;

        @NotNull(message = "정렬 순서는 필수입니다")
        private Integer sortOrder;
    }
}
