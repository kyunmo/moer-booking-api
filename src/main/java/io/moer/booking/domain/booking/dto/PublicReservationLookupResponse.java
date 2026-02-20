package io.moer.booking.domain.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.reservation.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 이름+전화번호 기반 예약 조회 응답 DTO (Public API)
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "이름+전화번호 기반 예약 조회 결과")
public class PublicReservationLookupResponse {

    @Schema(description = "예약 번호", example = "260220-A3B9")
    private String reservationNumber;

    @Schema(description = "예약 상태", example = "CONFIRMED")
    private ReservationStatus status;

    @Schema(description = "매장 이름", example = "모어 헤어살롱")
    private String businessName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "예약 날짜", example = "2026-02-25")
    private LocalDate reservationDate;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "시작 시간", example = "14:00")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "종료 시간", example = "15:00")
    private LocalTime endTime;

    @Schema(description = "담당 스태프 이름", example = "김직원")
    private String staffName;

    @Schema(description = "서비스 목록", example = "[\"커트\", \"염색\"]")
    private List<String> services;

    @Schema(description = "총 금액", example = "50000")
    private Integer totalPrice;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "예약 생성 시각")
    private LocalDateTime createdAt;
}
