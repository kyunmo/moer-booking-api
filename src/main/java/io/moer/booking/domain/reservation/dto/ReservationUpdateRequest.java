package io.moer.booking.domain.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 예약 수정 요청 DTO
 * 모든 필드 선택 (null이면 기존 값 유지)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationUpdateRequest {

    /**
     * 담당 직원 ID 변경 (null이면 유지)
     */
    private Long staffId;

    /**
     * 서비스 목록 변경 (null이면 유지)
     */
    private List<Long> serviceIds;

    /**
     * 예약 날짜 변경 (null이면 유지)
     */
    private LocalDate reservationDate;

    /**
     * 시작 시간 변경 (null이면 유지)
     */
    private LocalTime startTime;

    /**
     * 고객 요청사항 변경 (null이면 유지)
     */
    private String customerMemo;

    /**
     * 직원 메모 변경 (null이면 유지)
     */
    private String staffMemo;
}