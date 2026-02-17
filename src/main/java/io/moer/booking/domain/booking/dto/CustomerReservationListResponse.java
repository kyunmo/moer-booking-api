package io.moer.booking.domain.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.reservation.Reservation;
import io.swagger.v3.oas.annotations.media.Schema;
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
 * 로그인 고객용 예약 목록/상세 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "고객 예약 응답")
public class CustomerReservationListResponse {

    @Schema(description = "예약 번호", example = "260220-A3B9")
    private String reservationNumber;

    @Schema(description = "예약 상태", example = "CONFIRMED")
    private String status;

    @Schema(description = "매장 이름", example = "모어 헤어살롱")
    private String businessName;

    @Schema(description = "매장 슬러그", example = "moer-hair")
    private String businessSlug;

    @Schema(description = "매장 프로필 이미지 URL")
    private String businessProfileImageUrl;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "예약 날짜", example = "2026-02-20")
    private LocalDate reservationDate;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "시작 시간", example = "14:00")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "종료 시간", example = "15:00")
    private LocalTime endTime;

    @Schema(description = "담당 스태프 이름", example = "김디자이너")
    private String staffName;

    @Schema(description = "서비스 목록")
    private List<Map<String, Object>> services;

    @Schema(description = "총 가격", example = "50000")
    private Integer totalPrice;

    @Schema(description = "취소 가능 여부")
    private boolean canCancel;

    @Schema(description = "리뷰 작성 여부")
    private boolean hasReview;

    @Schema(description = "예약 생성일시")
    private LocalDateTime createdAt;

    /**
     * Reservation 엔티티 + 부가 정보를 통해 응답 DTO를 생성합니다.
     */
    public static CustomerReservationListResponse from(
            Reservation reservation, String businessName, String businessSlug,
            String businessProfileImageUrl, String staffName, boolean hasReview) {
        return CustomerReservationListResponse.builder()
                .reservationNumber(reservation.getReservationNumber())
                .status(reservation.getStatus().name())
                .businessName(businessName)
                .businessSlug(businessSlug)
                .businessProfileImageUrl(businessProfileImageUrl)
                .reservationDate(reservation.getReservationDate())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .staffName(staffName)
                .services(reservation.getServices())
                .totalPrice(reservation.getTotalPrice())
                .canCancel(reservation.canCancel())
                .hasReview(hasReview)
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}
