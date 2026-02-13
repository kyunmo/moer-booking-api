package io.moer.booking.domain.service.category.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.service.category.ServiceCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 서비스 카테고리 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class ServiceCategoryResponse {

    private Long id;
    private Long businessId;
    private String name;
    private String description;
    private Integer sortOrder;
    private Integer serviceCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Entity -> DTO 변환
     */
    public static ServiceCategoryResponse from(ServiceCategory category) {
        return ServiceCategoryResponse.builder()
                .id(category.getId())
                .businessId(category.getBusinessId())
                .name(category.getName())
                .description(category.getDescription())
                .sortOrder(category.getSortOrder())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    /**
     * Entity -> DTO 변환 (serviceCount 포함)
     */
    public static ServiceCategoryResponse from(ServiceCategory category, int serviceCount) {
        return ServiceCategoryResponse.builder()
                .id(category.getId())
                .businessId(category.getBusinessId())
                .name(category.getName())
                .description(category.getDescription())
                .sortOrder(category.getSortOrder())
                .serviceCount(serviceCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
