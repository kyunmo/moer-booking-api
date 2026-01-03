package io.moer.booking.domain.reservation;

/**
 * 예약 상태
 * DB 컬럼: reservations.status (VARCHAR(20))
 */
public enum ReservationStatus {
    /**
     * 대기 - 고객이 예약 신청, 관리자 승인 대기
     */
    PENDING,

    /**
     * 확정 - 관리자가 예약 승인
     */
    CONFIRMED,

    /**
     * 완료 - 서비스 제공 완료
     */
    COMPLETED,

    /**
     * 취소 - 고객 또는 관리자가 예약 취소
     */
    CANCELLED,

    /**
     * 노쇼 - 예약 시간에 고객이 나타나지 않음
     */
    NO_SHOW
}