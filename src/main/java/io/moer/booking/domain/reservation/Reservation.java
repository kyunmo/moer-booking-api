package io.moer.booking.domain.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    private Long id;
    private Long businessId;
    private Long customerId;
    private Long staffId;

    // 예약 정보
    private String reservationNumber;
    private LocalDate reservationDate;
    private LocalTime startTime;
    private LocalTime endTime;

    // 서비스
    private List<Long> serviceIds;
    private List<String> serviceNames;
    private Integer totalDuration;  // 총 소요 시간 (분)
    private Integer totalPrice;

    // 상태
    private ReservationStatus status;

    // 메모
    private String customerRequest;
    private String adminMemo;

    // 알림
    private Map<String, Object> notificationSent;

    // 취소 정보
    private LocalDateTime cancelledAt;
    private String cancelReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 예약 확정
     */
    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    /**
     * 예약 완료
     */
    public void complete() {
        this.status = ReservationStatus.COMPLETED;
    }

    /**
     * 예약 취소
     */
    public void cancel(String reason) {
        this.status = ReservationStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
    }

    /**
     * 노쇼 처리
     */
    public void noshow() {
        this.status = ReservationStatus.NOSHOW;
    }

    /**
     * 예약 확정 가능 여부
     */
    public boolean canConfirm() {
        return status == ReservationStatus.PENDING;
    }

    /**
     * 예약 취소 가능 여부
     */
    public boolean canCancel() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED;
    }

    /**
     * 예약 완료 가능 여부
     */
    public boolean canComplete() {
        return status == ReservationStatus.CONFIRMED;
    }

    /**
     * 예약이 활성 상태인지 확인
     */
    public boolean isActive() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED;
    }
}