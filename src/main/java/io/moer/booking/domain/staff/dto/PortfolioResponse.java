package io.moer.booking.domain.staff.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.staff.Portfolio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 포트폴리오 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class PortfolioResponse {

    private Long id;
    private Long staffId;
    private Long businessId;

    private String title;
    private String description;
    private String imageUrl;

    /**
     * 태그 목록
     * DB의 콤마 구분 문자열을 List로 변환
     */
    private List<String> tags;

    private Integer displayOrder;

    /**
     * 공개 여부
     * DB의 Y/N을 boolean으로 변환
     */
    private Boolean isVisible;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Entity → DTO 변환
     */
    public static PortfolioResponse from(Portfolio portfolio) {
        return PortfolioResponse.builder()
                .id(portfolio.getId())
                .staffId(portfolio.getStaffId())
                .businessId(portfolio.getBusinessId())
                .title(portfolio.getTitle())
                .description(portfolio.getDescription())
                .imageUrl(portfolio.getImageUrl())
                .tags(portfolio.getTagList())  // String → List 변환
                .displayOrder(null)  // 현재 미사용
                .isVisible("Y".equals(portfolio.getIsVisible()))  // Y/N → boolean
                .createdAt(portfolio.getCreatedAt())
                .build();
    }
}