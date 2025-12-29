package io.moer.booking.domain.reservation;

public enum ReservationStatus {
    PENDING,     // 대기 (고객이 신청)
    CONFIRMED,   // 확정 (관리자 승인)
    COMPLETED,   // 완료 (시술 끝)
    CANCELLED,   // 취소 (고객/관리자)
    NOSHOW       // 노쇼 (예약 시간에 나타나지 않음)
}