package io.moer.booking.domain.review.dto;

import io.moer.booking.domain.review.Review;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public 리뷰 조회 응답 (고객명 마스킹)
 */
@Getter
@Builder
@Schema(description = "리뷰 응답 (Public)")
public class ReviewResponse {

    @Schema(description = "리뷰 ID")
    private Long id;

    @Schema(description = "매장 이름", example = "모어 헤어살롱")
    private String businessName;

    @Schema(description = "매장 슬러그", example = "moer-hair")
    private String businessSlug;

    @Schema(description = "고객명 (마스킹)", example = "박*연")
    private String customerName;

    @Schema(description = "별점", example = "5")
    private Integer rating;

    @Schema(description = "리뷰 내용")
    private String content;

    @Schema(description = "서비스명")
    private String serviceName;

    @Schema(description = "담당 스태프명")
    private String staffName;

    @Schema(description = "리뷰 이미지 URL 목록")
    private List<String> images;

    @Schema(description = "작성일시")
    private LocalDateTime createdAt;

    @Schema(description = "사장님 답변")
    private ReplyInfo reply;

    /**
     * Entity -> Response 변환 (Public, 마스킹 적용)
     */
    public static ReviewResponse from(Review review, String serviceName, String staffName) {
        return from(review, serviceName, staffName, null, null);
    }

    /**
     * Entity -> Response 변환 (businessName, businessSlug 포함)
     */
    public static ReviewResponse from(Review review, String serviceName, String staffName,
                                       String businessName, String businessSlug) {
        ReplyInfo replyInfo = null;
        if (review.getReplyContent() != null && !review.getReplyContent().isBlank()) {
            replyInfo = new ReplyInfo(review.getReplyContent(), review.getReplyCreatedAt());
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .businessName(businessName)
                .businessSlug(businessSlug)
                .customerName(maskName(review.getCustomerName()))
                .rating(review.getRating())
                .content(review.getContent())
                .serviceName(serviceName)
                .staffName(staffName)
                .images(review.getImages())
                .createdAt(review.getCreatedAt())
                .reply(replyInfo)
                .build();
    }

    /**
     * 고객명 마스킹 로직
     * 1글자: "*"
     * 2글자: 첫글자 + "*"
     * 3글자 이상: 첫글자 + "*".repeat(len-2) + 마지막글자
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return "*";
        }
        int len = name.length();
        if (len == 1) {
            return "*";
        }
        if (len == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + "*".repeat(len - 2) + name.charAt(len - 1);
    }

    @Getter
    @Schema(description = "사장님 답변 정보")
    public static class ReplyInfo {

        @Schema(description = "답변 내용")
        private final String content;

        @Schema(description = "답변 작성일시")
        private final LocalDateTime createdAt;

        public ReplyInfo(String content, LocalDateTime createdAt) {
            this.content = content;
            this.createdAt = createdAt;
        }
    }
}
