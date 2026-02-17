package io.moer.booking.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "리뷰 삭제 요청")
public class ReviewDeleteRequest {

    @Schema(description = "삭제 사유 (선택)", example = "부적절한 내용")
    private String reason;
}
