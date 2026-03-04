package io.moer.booking.domain.review.repository;

import io.moer.booking.domain.review.ReviewImage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

/**
 * 리뷰 이미지 Repository
 */
@Mapper
public interface ReviewImageRepository {

    /**
     * 리뷰 이미지 저장
     */
    void save(ReviewImage image);

    /**
     * ID로 조회
     */
    Optional<ReviewImage> findById(Long id);

    /**
     * 리뷰 ID로 이미지 목록 조회
     */
    List<ReviewImage> findByReviewId(Long reviewId);

    /**
     * 리뷰 ID로 이미지 수 조회
     */
    int countByReviewId(Long reviewId);

    /**
     * ID로 삭제
     */
    void deleteById(Long id);

    /**
     * 리뷰 ID로 전체 이미지 삭제
     */
    void deleteByReviewId(Long reviewId);
}
