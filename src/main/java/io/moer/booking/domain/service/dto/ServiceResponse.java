package io.moer.booking.domain.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.service.Service;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class ServiceResponse {

    private Long id;
    private Long businessId;

    private String category;
    private String name;
    private String description;

    private Integer price;
    private Integer duration;

    private String imageUrl;
    private Map<String, Object> options;
    private List<Long> availableStaffIds;

    private Boolean isActive;
    private Integer displayOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public static ServiceResponse from(Service service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .businessId(service.getBusinessId())
                .category(service.getCategory())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .duration(service.getDuration())
                .imageUrl(service.getImageUrl())
                .options(service.getOptions())
                .availableStaffIds(service.getAvailableStaffIds())
                .isActive(service.getIsActive())
                .displayOrder(service.getDisplayOrder())
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .build();
    }
}