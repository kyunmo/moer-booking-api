package io.moer.booking.domain.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 로그인 고객용 예약 생성 요청 DTO
 * 비인증 Public API와 달리 이름/전화번호/이메일은 User 정보에서 자동으로 가져옵니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그인 고객용 예약 생성 요청")
public class CustomerReservationCreateRequest {

    @NotEmpty(message = "서비스를 선택해주세요")
    @Schema(description = "서비스 ID 목록", example = "[1, 2]")
    private List<Long> serviceIds;

    @Schema(description = "담당 스태프 ID (미지정 시 자동 배정)", example = "1")
    private Long staffId;

    @NotNull(message = "예약 날짜를 선택해주세요")
    @Schema(description = "예약 날짜", example = "2026-02-20")
    private LocalDate reservationDate;

    @NotNull(message = "예약 시간을 선택해주세요")
    @Schema(description = "예약 시간", example = "14:00")
    private LocalTime startTime;

    @Schema(description = "고객 요청사항 (선택)", example = "조용한 자리 부탁드립니다")
    private String customerRequest;
}
