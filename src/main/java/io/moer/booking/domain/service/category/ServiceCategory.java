package io.moer.booking.domain.service.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 서비스 카테고리 엔티티
 * DB 테이블: service_categories
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCategory {
    private Long id;
    private Long businessId;
    private String name;
    private String description;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
