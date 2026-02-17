package io.moer.booking.domain.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 리뷰 엔티티
 * DB 테이블: reviews
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    private Long id;
    private Long businessId;
    private Long reservationId;
    private Long customerId;
    private Long staffId;
    private String customerName;
    private String customerPhone;
    private Integer rating;
    private String content;

    /**
     * 이미지 URL 목록 (JSONB)
     * DB: JSONB
     * 예: ["https://cdn.moer.io/reviews/img1.jpg", ...]
     */
    private List<String> images;

    /**
     * 리뷰 상태 (Enum)
     * DB: VARCHAR(20)
     */
    private ReviewStatus status;

    private String replyContent;
    private LocalDateTime replyCreatedAt;
    private String deleteReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========================================
    // 헬퍼 메서드 - 상태 체크
    // ========================================

    /**
     * 활성 리뷰 여부
     */
    public boolean isActive() {
        return ReviewStatus.ACTIVE.equals(this.status);
    }

    /**
     * 숨김 리뷰 여부
     */
    public boolean isHidden() {
        return ReviewStatus.HIDDEN.equals(this.status);
    }

    /**
     * 삭제된 리뷰 여부
     */
    public boolean isDeleted() {
        return ReviewStatus.DELETED.equals(this.status);
    }

    /**
     * 답변이 등록된 리뷰 여부
     */
    public boolean isReplied() {
        return replyContent != null && !replyContent.isBlank();
    }
}
