package io.moer.booking.domain.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 포트폴리오 엔티티
 * DB 테이블: portfolios
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio {
    private Long id;
    private Long staffId;

    /**
     * Business ID (JOIN으로 조회)
     * portfolios 테이블에는 없지만, DTO에서 필요
     */
    private Long businessId;

    private String title;
    private String description;
    private String imageUrl;

    /**
     * 태그 (콤마 구분 문자열)
     * DB: TEXT
     * 예: "컷,펌,염색"
     */
    private String tags;

    /**
     * 공개 여부 (Y/N)
     * DB: CHAR(1)
     */
    private String isVisible;

    private LocalDateTime createdAt;

    // ========================================
    // 헬퍼 메서드
    // ========================================

    /**
     * 공개 상태 확인
     */
    public boolean isVisible() {
        return "Y".equals(this.isVisible);
    }

    /**
     * tags 문자열을 List<String>으로 변환
     * "컷,펌,염색" → ["컷", "펌", "염색"]
     */
    public List<String> getTagList() {
        if (tags == null || tags.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * List<String>을 tags 문자열로 변환
     * ["컷", "펌", "염색"] → "컷,펌,염색"
     */
    public static String tagsToString(List<String> tagList) {
        if (tagList == null || tagList.isEmpty()) {
            return null;
        }
        return String.join(",", tagList);
    }
}