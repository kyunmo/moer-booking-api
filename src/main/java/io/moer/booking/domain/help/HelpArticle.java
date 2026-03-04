package io.moer.booking.domain.help;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 도움말 엔티티
 * DB 테이블: help_articles
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HelpArticle {

    private Long id;
    private String category;
    private String title;
    private String content;
    private String relatedFeature;
    private Integer sortOrder;
    private String lang;
    private Boolean isPublished;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========================================
    // 헬퍼 메서드
    // ========================================

    public boolean isPublished() {
        return Boolean.TRUE.equals(this.isPublished);
    }
}
