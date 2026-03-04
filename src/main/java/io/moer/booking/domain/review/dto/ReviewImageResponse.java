package io.moer.booking.domain.review.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.review.ReviewImage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "리뷰 이미지 응답")
public class ReviewImageResponse {

    @Schema(description = "이미지 ID")
    private Long id;

    @Schema(description = "리뷰 ID")
    private Long reviewId;

    @Schema(description = "이미지 URL")
    private String imageUrl;

    @Schema(description = "썸네일 URL")
    private String thumbnailUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    public static ReviewImageResponse from(ReviewImage image) {
        return ReviewImageResponse.builder()
                .id(image.getId())
                .reviewId(image.getReviewId())
                .imageUrl(image.getImageUrl())
                .thumbnailUrl(image.getThumbnailUrl())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
