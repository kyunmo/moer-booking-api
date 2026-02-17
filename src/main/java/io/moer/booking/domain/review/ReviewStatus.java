package io.moer.booking.domain.review;

/**
 * 리뷰 상태
 * DB 컬럼: reviews.status (VARCHAR(20))
 */
public enum ReviewStatus {
    /**
     * 활성 - 정상적으로 노출되는 리뷰
     */
    ACTIVE,

    /**
     * 숨김 - 관리자가 숨김 처리한 리뷰
     */
    HIDDEN,

    /**
     * 삭제 - 소프트 삭제된 리뷰
     */
    DELETED
}
