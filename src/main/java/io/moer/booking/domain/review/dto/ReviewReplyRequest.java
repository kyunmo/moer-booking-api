package io.moer.booking.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "리뷰 답변 요청")
public class ReviewReplyRequest {

    @NotBlank(message = "답변 내용은 필수입니다")
    @Schema(description = "답변 내용", example = "방문해 주셔서 감사합니다. 또 뵙겠습니다!")
    private String content;
}
