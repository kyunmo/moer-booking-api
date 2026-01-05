package io.moer.booking.domain.reservation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 예약 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCreateRequest {

    /**
     * 기존 고객 ID (있으면 사용, 없으면 자동 생성)
     */
    private Long customerId;

    /**
     * 고객 이름 (customerId가 없을 때 필수)
     */
    private String customerName;

    /**
     * 고객 전화번호 (customerId가 없을 때 필수)
     */
    @Pattern(regexp = "^$|^010-\\d{4}-\\d{4}$",
            message = "전화번호 형식이 올바르지 않습니다 (예: 010-1234-5678)")
    private String customerPhone;

    /**
     * 담당 직원 ID (NULL 가능)
     */
    private Long staffId;

    /**
     * 서비스 ID 목록 (최소 1개 이상)
     */
    @NotEmpty(message = "서비스는 최소 1개 이상 선택해야 합니다")
    private List<Long> serviceIds;

    /**
     * 예약 날짜
     */
    @NotNull(message = "예약 날짜는 필수입니다")
    private LocalDate reservationDate;

    /**
     * 시작 시간
     */
    @NotNull(message = "시작 시간은 필수입니다")
    private LocalTime startTime;

    /**
     * 고객 요청사항 (선택)
     */
    private String customerMemo;
}