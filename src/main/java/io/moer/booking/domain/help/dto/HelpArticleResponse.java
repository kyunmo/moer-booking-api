package io.moer.booking.domain.help.dto;

import io.moer.booking.domain.help.HelpArticle;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 도움말 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "도움말 응답")
public class HelpArticleResponse {

    @Schema(description = "도움말 ID", example = "1")
    private Long id;

    @Schema(description = "카테고리", example = "reservation")
    private String category;

    @Schema(description = "제목", example = "예약을 생성하려면 어떻게 하나요?")
    private String title;

    @Schema(description = "본문 (마크다운)", example = "## 예약 생성\n1. ...")
    private String content;

    @Schema(description = "관련 기능 식별자", example = "reservation.create")
    private String relatedFeature;

    @Schema(description = "정렬 순서", example = "0")
    private Integer sortOrder;

    @Schema(description = "언어", example = "ko")
    private String lang;

    @Schema(description = "공개 여부", example = "true")
    private Boolean isPublished;

    @Schema(description = "생성일")
    private LocalDateTime createdAt;

    @Schema(description = "수정일")
    private LocalDateTime updatedAt;

    /**
     * Entity -> DTO 변환
     */
    public static HelpArticleResponse from(HelpArticle article) {
        return HelpArticleResponse.builder()
                .id(article.getId())
                .category(article.getCategory())
                .title(article.getTitle())
                .content(article.getContent())
                .relatedFeature(article.getRelatedFeature())
                .sortOrder(article.getSortOrder())
                .lang(article.getLang())
                .isPublished(article.getIsPublished())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }
}
