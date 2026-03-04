package io.moer.booking.domain.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.service.ServiceImage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "서비스 이미지 응답")
public class ServiceImageResponse {

    @Schema(description = "이미지 ID")
    private Long id;

    @Schema(description = "서비스 ID")
    private Long serviceId;

    @Schema(description = "매장 ID")
    private Long businessId;

    @Schema(description = "이미지 URL")
    private String imageUrl;

    @Schema(description = "썸네일 URL")
    private String thumbnailUrl;

    @Schema(description = "원본 파일명")
    private String originalFilename;

    @Schema(description = "파일 크기 (bytes)")
    private Long fileSize;

    @Schema(description = "정렬 순서")
    private Integer sortOrder;

    @Schema(description = "캡션")
    private String caption;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    public static ServiceImageResponse from(ServiceImage image) {
        return ServiceImageResponse.builder()
                .id(image.getId())
                .serviceId(image.getServiceId())
                .businessId(image.getBusinessId())
                .imageUrl(image.getImageUrl())
                .thumbnailUrl(image.getThumbnailUrl())
                .originalFilename(image.getOriginalFilename())
                .fileSize(image.getFileSize())
                .sortOrder(image.getSortOrder())
                .caption(image.getCaption())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
