package io.moer.booking.domain.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.reservation.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 고객용 예약 생성 결과 응답 DTO (Public API)
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "예약 생성 결과")
public class PublicReservationResponse {

    @Schema(description = "예약 번호", example = "260220-A3B9")
    private String reservationNumber;

    @Schema(description = "예약 상태 (PENDING 또는 CONFIRMED)")
    private ReservationStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "예약 날짜", example = "2026-02-20")
    private LocalDate reservationDate;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "시작 시간", example = "14:00")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "종료 시간", example = "15:00")
    private LocalTime endTime;

    @Schema(description = "안내 메시지", example = "예약이 완료되었습니다. 예약번호: 260220-A3B9")
    private String message;
}
