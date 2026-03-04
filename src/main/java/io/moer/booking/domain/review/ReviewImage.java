package io.moer.booking.domain.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 리뷰 이미지 엔티티
 * DB 테이블: review_images
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewImage {
    private Long id;
    private Long reviewId;
    private String imageUrl;
    private String thumbnailUrl;
    private String originalFilename;
    private Integer fileSize;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
