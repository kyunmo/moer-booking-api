package io.moer.booking.domain.help.dto;

import io.moer.booking.domain.help.HelpCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 도움말 목록 응답 DTO (카테고리 + 아이템 + 총 개수)
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "도움말 목록 응답")
public class HelpListResponse {

    @Schema(description = "카테고리 목록")
    private List<CategoryItem> categories;

    @Schema(description = "도움말 아이템 목록")
    private List<HelpArticleResponse> items;

    @Schema(description = "총 개수", example = "42")
    private int totalCount;

    /**
     * 카테고리 아이템
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "카테고리 정보")
    public static class CategoryItem {

        @Schema(description = "카테고리 ID", example = "reservation")
        private String id;

        @Schema(description = "카테고리 이름", example = "예약 관리")
        private String name;

        @Schema(description = "카테고리 아이콘", example = "mdi-calendar")
        private String icon;
    }

    /**
     * 전체 카테고리 목록 생성
     */
    public static List<CategoryItem> allCategories() {
        return Arrays.stream(HelpCategory.values())
                .map(c -> CategoryItem.builder()
                        .id(c.name().toLowerCase())
                        .name(c.getName())
                        .icon(c.getIcon())
                        .build())
                .toList();
    }
}
