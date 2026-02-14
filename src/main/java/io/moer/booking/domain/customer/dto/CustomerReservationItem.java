package io.moer.booking.domain.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 고객 예약 이력 - 개별 예약 항목
 */
@Getter
@Builder
@Schema(description = "고객 예약 이력 항목")
public class CustomerReservationItem {

    @Schema(description = "예약 ID", example = "101")
    private Long id;

    @Schema(description = "예약 날짜", example = "2026-02-10")
    private LocalDate reservationDate;

    @Schema(description = "시작 시간", example = "14:00")
    private LocalTime startTime;

    @Schema(description = "종료 시간", example = "15:30")
    private LocalTime endTime;

    @Schema(description = "담당 직원명", example = "김디자이너")
    private String staffName;

    @Schema(description = "서비스명 목록", example = "[\"여성컷\", \"트리트먼트\"]")
    private List<String> services;

    @Schema(description = "총 금액", example = "50000")
    private Integer totalPrice;

    @Schema(description = "예약 상태", example = "COMPLETED")
    private String status;
}
