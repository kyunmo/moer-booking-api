package io.moer.booking.domain.review.dto;

import io.moer.booking.domain.review.Review;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin 리뷰 관리 응답 (고객 전체 정보 포함)
 */
@Getter
@Builder
@Schema(description = "리뷰 응답 (Admin)")
public class ReviewAdminResponse {

    @Schema(description = "리뷰 ID")
    private Long id;

    @Schema(description = "고객명 (전체)")
    private String customerName;

    @Schema(description = "고객 전화번호")
    private String customerPhone;

    @Schema(description = "예약 ID")
    private Long reservationId;

    @Schema(description = "별점")
    private Integer rating;

    @Schema(description = "리뷰 내용")
    private String content;

    @Schema(description = "서비스명")
    private String serviceName;

    @Schema(description = "담당 스태프 ID")
    private Long staffId;

    @Schema(description = "담당 스태프명")
    private String staffName;

    @Schema(description = "리뷰 이미지 URL 목록")
    private List<String> images;

    @Schema(description = "답변 여부")
    private Boolean isReplied;

    @Schema(description = "사장님 답변")
    private ReviewResponse.ReplyInfo reply;

    @Schema(description = "리뷰 상태")
    private String status;

    @Schema(description = "삭제 사유")
    private String deleteReason;

    @Schema(description = "작성일시")
    private LocalDateTime createdAt;

    /**
     * Entity -> AdminResponse 변환
     */
    public static ReviewAdminResponse from(Review review, String serviceName, String staffName) {
        ReviewResponse.ReplyInfo replyInfo = null;
        if (review.getReplyContent() != null && !review.getReplyContent().isBlank()) {
            replyInfo = new ReviewResponse.ReplyInfo(review.getReplyContent(), review.getReplyCreatedAt());
        }

        return ReviewAdminResponse.builder()
                .id(review.getId())
                .customerName(review.getCustomerName())
                .customerPhone(review.getCustomerPhone())
                .reservationId(review.getReservationId())
                .rating(review.getRating())
                .content(review.getContent())
                .serviceName(serviceName)
                .staffId(review.getStaffId())
                .staffName(staffName)
                .images(review.getImages())
                .isReplied(review.isReplied())
                .reply(replyInfo)
                .status(review.getStatus() != null ? review.getStatus().name() : null)
                .deleteReason(review.getDeleteReason())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
