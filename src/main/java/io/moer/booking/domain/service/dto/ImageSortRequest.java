package io.moer.booking.domain.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "이미지 정렬 순서 변경 요청")
public class ImageSortRequest {

    @NotEmpty(message = "정렬 목록은 비어있을 수 없습니다")
    @Valid
    @Schema(description = "이미지 정렬 목록")
    private List<ImageOrder> imageOrders;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "이미지 정렬 항목")
    public static class ImageOrder {

        @NotNull(message = "이미지 ID는 필수입니다")
        @Schema(description = "이미지 ID")
        private Long imageId;

        @NotNull(message = "정렬 순서는 필수입니다")
        @Schema(description = "정렬 순서")
        private Integer sortOrder;
    }
}
