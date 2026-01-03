package io.moer.booking.domain.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.reservation.Reservation;
import io.moer.booking.domain.reservation.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 예약 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class ReservationResponse {

    private Long id;
    private Long businessId;
    private Long customerId;
    private Long staffId;

    // 고객/직원 이름 (조인 필요 - 추후 구현)
    private String customerName;
    private String staffName;

    private String reservationNumber;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reservationDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    // 서비스 정보
    private List<Long> serviceIds;
    private List<String> serviceNames;
    private Integer totalDuration;
    private Integer totalPrice;

    private ReservationStatus status;

    // 메모
    private String customerMemo;  // 고객 요청사항
    private String staffMemo;     // 직원 메모

    // 취소 정보
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelledAt;
    private String cancelReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static ReservationResponse from(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .businessId(reservation.getBusinessId())
                .customerId(reservation.getCustomerId())
                .staffId(reservation.getStaffId())
                .reservationNumber(reservation.getReservationNumber())
                .reservationDate(reservation.getReservationDate())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .serviceIds(reservation.getServiceIds())
                .serviceNames(reservation.getServiceNames())
                .totalDuration(reservation.getTotalDuration())
                .totalPrice(reservation.getTotalPrice())
                .status(reservation.getStatus())
                .customerMemo(reservation.getCustomerMemo())
                .staffMemo(reservation.getStaffMemo())
                .cancelledAt(reservation.getCancelledAt())
                .cancelReason(reservation.getCancelReason())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }
}