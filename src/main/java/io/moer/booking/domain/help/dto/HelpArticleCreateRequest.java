package io.moer.booking.domain.help.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 도움말 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "도움말 생성/수정 요청")
public class HelpArticleCreateRequest {

    @NotBlank(message = "카테고리는 필수입니다")
    @Size(max = 50, message = "카테고리는 50자 이하여야 합니다")
    @Schema(description = "카테고리", example = "reservation")
    private String category;

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다")
    @Schema(description = "제목", example = "예약을 생성하려면 어떻게 하나요?")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    @Schema(description = "본문 (마크다운)", example = "## 예약 생성\n1. ...")
    private String content;

    @Size(max = 100, message = "관련 기능 식별자는 100자 이하여야 합니다")
    @Schema(description = "관련 기능 식별자", example = "reservation.create")
    private String relatedFeature;

    @Schema(description = "정렬 순서", example = "0")
    private Integer sortOrder;

    @Size(max = 10, message = "언어 코드는 10자 이하여야 합니다")
    @Schema(description = "언어 코드", example = "ko")
    private String lang;

    @Schema(description = "공개 여부", example = "true")
    private Boolean isPublished;
}
