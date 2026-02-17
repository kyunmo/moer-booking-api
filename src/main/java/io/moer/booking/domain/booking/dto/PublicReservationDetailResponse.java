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
 * 고객용 예약 상세 조회 응답 DTO (Public API)
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "예약 상세 조회 결과")
public class PublicReservationDetailResponse {

    @Schema(description = "예약 번호", example = "260220-A3B9")
    private String reservationNumber;

    @Schema(description = "예약 상태")
    private ReservationStatus status;

    // 매장 정보
    @Schema(description = "매장 이름", example = "모어 헤어살롱")
    private String businessName;

    @Schema(description = "매장 주소", example = "서울시 강남구 역삼동 123")
    private String businessAddress;

    @Schema(description = "매장 전화번호", example = "02-1234-5678")
    private String businessPhone;

    // 예약 정보
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "예약 날짜", example = "2026-02-20")
    private LocalDate reservationDate;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "시작 시간", example = "14:00")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "종료 시간", example = "15:00")
    private LocalTime endTime;

    @Schema(description = "담당 스태프 이름", example = "김미소")
    private String staffName;

    @Schema(description = "서비스 목록", example = "[\"커트\", \"염색\"]")
    private List<String> services;

    @Schema(description = "총 금액", example = "50000")
    private Integer totalPrice;

    @Schema(description = "총 소요 시간 (분)", example = "90")
    private Integer totalDuration;

    // 취소 관련
    @Schema(description = "취소 가능 여부")
    private Boolean canCancel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "취소 기한")
    private LocalDateTime cancelDeadline;

    @Schema(description = "고객 메모", example = "조용한 자리 부탁드립니다")
    private String customerMemo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "예약 생성 시각")
    private LocalDateTime createdAt;
}
