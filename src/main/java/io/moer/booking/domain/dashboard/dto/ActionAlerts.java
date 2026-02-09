package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 실시간 액션 알림
 */
@Getter
@Builder
@AllArgsConstructor
public class ActionAlerts {
    /**
     * 확정 대기중 예약 수 (PENDING 상태)
     */
    private Integer pendingReservations;

    /**
     * 1시간 이내 시작 예약 수
     */
    private Integer upcomingReservations;

    /**
     * 오늘 생일 고객 수
     */
    private Integer birthdayCustomers;

    /**
     * 재방문 유도 대상 고객 수 (1개월 이상 미방문)
     */
    private Integer inactiveCustomers;

    /**
     * 전체 알림 수
     */
    public Integer getTotalAlerts() {
        return (pendingReservations != null ? pendingReservations : 0) +
                (upcomingReservations != null ? upcomingReservations : 0) +
                (birthdayCustomers != null ? birthdayCustomers : 0) +
                (inactiveCustomers != null ? inactiveCustomers : 0);
    }
}
