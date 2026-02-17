package io.moer.booking.domain.review.repository;

import io.moer.booking.domain.review.Review;
import io.moer.booking.domain.review.dto.ReviewSearchCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 리뷰 Repository
 */
@Mapper
public interface ReviewRepository {

    // ========================================
    // CUD
    // ========================================

    /**
     * 리뷰 저장
     */
    void save(Review review);

    /**
     * 리뷰 답변 등록
     */
    void updateReply(@Param("id") Long id,
                     @Param("replyContent") String replyContent,
                     @Param("replyCreatedAt") java.time.LocalDateTime replyCreatedAt);

    /**
     * 리뷰 상태 변경 (소프트 삭제 등)
     */
    void updateStatus(@Param("id") Long id,
                      @Param("status") String status,
                      @Param("deleteReason") String deleteReason);

    // ========================================
    // 조회 - 단건
    // ========================================

    /**
     * ID로 조회
     */
    Optional<Review> findById(Long id);

    /**
     * 예약 ID로 조회
     */
    Optional<Review> findByReservationId(Long reservationId);

    /**
     * 예약 ID로 리뷰 존재 여부 확인
     */
    boolean existsByReservationId(Long reservationId);

    // ========================================
    // 조회 - 목록 (Admin)
    // ========================================

    /**
     * Admin용 리뷰 목록 (동적 필터 + 페이징)
     */
    List<Review> findByBusinessId(ReviewSearchCondition condition);

    /**
     * Admin용 리뷰 총 개수 (동적 필터)
     */
    int countByBusinessId(ReviewSearchCondition condition);

    // ========================================
    // 조회 - 목록 (Public)
    // ========================================

    /**
     * Public용 리뷰 목록 (status=ACTIVE + 페이징)
     */
    List<Review> findPublicByBusinessId(@Param("businessId") Long businessId,
                                        @Param("rating") Integer rating,
                                        @Param("staffId") Long staffId,
                                        @Param("sortBy") String sortBy,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    /**
     * Public용 리뷰 총 개수 (status=ACTIVE)
     */
    int countPublicByBusinessId(@Param("businessId") Long businessId,
                                @Param("rating") Integer rating,
                                @Param("staffId") Long staffId);

    // ========================================
    // 통계
    // ========================================

    /**
     * 매장의 평균 평점 (ACTIVE 상태만)
     */
    Double getAverageRatingByBusinessId(Long businessId);

    /**
     * 매장의 활성 리뷰 수
     */
    int countActiveByBusinessId(Long businessId);

    /**
     * 매장의 미답변 리뷰 수
     */
    int countUnrepliedByBusinessId(Long businessId);

    /**
     * 매장의 이번 달 리뷰 수
     */
    int countThisMonthByBusinessId(Long businessId);

    /**
     * 별점 분포 (rating, count)
     */
    List<Map<String, Object>> getRatingDistribution(Long businessId);
}
