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

/**
 * 예약 엔티티
 * DB 테이블: reservations
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {
    private Long id;
    private Long businessId;
    private Long customerId;
    private Long userId;       // 로그인 고객 사용자 ID
    private Long staffId;
    private LocalDate reservationDate;
    private LocalTime startTime;
    private LocalTime endTime;

    // JSONB 컬럼 - services 정보를 담는 List
    // [{id: 1, name: "커트", price: 20000, duration: 30}, ...]
    private List<Map<String, Object>> services;

    private Integer totalDuration;
    private Integer totalPrice;

    // Enum 타입으로 변경
    private ReservationStatus status;

    private String reservationNumber;
    private String source;        // 예약 출처 (ADMIN, ONLINE)
    private String customerMemo;  // 고객 요청사항
    private String staffMemo;     // 직원 메모
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========================================
    // 헬퍼 메서드 - 상태 체크
    // ========================================

    public boolean isPending() {
        return ReservationStatus.PENDING.equals(this.status);
    }

    public boolean isConfirmed() {
        return ReservationStatus.CONFIRMED.equals(this.status);
    }

    public boolean isCompleted() {
        return ReservationStatus.COMPLETED.equals(this.status);
    }

    public boolean isCancelled() {
        return ReservationStatus.CANCELLED.equals(this.status);
    }

    public boolean isNoShow() {
        return ReservationStatus.NO_SHOW.equals(this.status);
    }

    // ========================================
    // 비즈니스 로직 - 상태 전환 가능 여부
    // ========================================

    /**
     * 확정 가능 여부
     * PENDING → CONFIRMED
     */
    public boolean canConfirm() {
        return ReservationStatus.PENDING.equals(this.status);
    }

    /**
     * 완료 가능 여부
     * CONFIRMED → COMPLETED
     */
    public boolean canComplete() {
        return ReservationStatus.CONFIRMED.equals(this.status);
    }

    /**
     * 취소 가능 여부
     * PENDING, CONFIRMED, COMPLETED → CANCELLED
     * (COMPLETED 취소 시 고객 통계 롤백 필요)
     */
    public boolean canCancel() {
        return ReservationStatus.PENDING.equals(this.status) ||
                ReservationStatus.CONFIRMED.equals(this.status) ||
                ReservationStatus.COMPLETED.equals(this.status);
    }

    /**
     * 노쇼 처리 가능 여부
     * CONFIRMED → NO_SHOW
     */
    public boolean canMarkAsNoShow() {
        return ReservationStatus.CONFIRMED.equals(this.status);
    }

    // ========================================
    // 헬퍼 메서드 - services JSONB 추출
    // ========================================

    /**
     * services JSONB에서 service ID 목록 추출
     */
    public List<Long> getServiceIds() {
        if (services == null || services.isEmpty()) {
            return List.of();
        }

        return services.stream()
                .map(service -> {
                    Object id = service.get("id");
                    if (id instanceof Integer) {
                        return ((Integer) id).longValue();
                    } else if (id instanceof Long) {
                        return (Long) id;
                    }
                    return null;
                })
                .filter(id -> id != null)
                .toList();
    }

    /**
     * services JSONB에서 service 이름 목록 추출
     */
    public List<String> getServiceNames() {
        if (services == null || services.isEmpty()) {
            return List.of();
        }

        return services.stream()
                .map(service -> (String) service.get("name"))
                .filter(name -> name != null)
                .toList();
    }
}