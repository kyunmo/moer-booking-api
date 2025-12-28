package io.moer.booking.domain.staff.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.staff.Portfolio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

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
    private List<String> tags;

    private Integer displayOrder;
    private Boolean isVisible;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public static PortfolioResponse from(Portfolio portfolio) {
        return PortfolioResponse.builder()
                .id(portfolio.getId())
                .staffId(portfolio.getStaffId())
                .businessId(portfolio.getBusinessId())
                .title(portfolio.getTitle())
                .description(portfolio.getDescription())
                .imageUrl(portfolio.getImageUrl())
                .tags(portfolio.getTags())
                .displayOrder(portfolio.getDisplayOrder())
                .isVisible(portfolio.getIsVisible())
                .createdAt(portfolio.getCreatedAt())
                .build();
    }
}