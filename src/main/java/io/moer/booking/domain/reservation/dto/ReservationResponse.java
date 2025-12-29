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
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class ReservationResponse {

    private Long id;
    private Long businessId;
    private Long customerId;
    private Long staffId;

    // 고객/직원 이름 (조인 필요 - 추후)
    private String customerName;
    private String staffName;

    private String reservationNumber;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reservationDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private List<Long> serviceIds;
    private List<String> serviceNames;
    private Integer totalDuration;
    private Integer totalPrice;

    private ReservationStatus status;

    private String customerRequest;
    private String adminMemo;

    private Map<String, Object> notificationSent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelledAt;
    private String cancelReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

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
                .customerRequest(reservation.getCustomerRequest())
                .adminMemo(reservation.getAdminMemo())
                .notificationSent(reservation.getNotificationSent())
                .cancelledAt(reservation.getCancelledAt())
                .cancelReason(reservation.getCancelReason())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }
}