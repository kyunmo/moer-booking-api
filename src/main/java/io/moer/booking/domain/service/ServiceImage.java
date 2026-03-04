package io.moer.booking.domain.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 서비스 이미지 엔티티
 * DB 테이블: service_images
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceImage {
    private Long id;
    private Long serviceId;
    private Long businessId;
    private String imageUrl;
    private String thumbnailUrl;
    private String originalFilename;
    private Long fileSize;
    private Integer sortOrder;
    private String caption;
    private LocalDateTime createdAt;
}
