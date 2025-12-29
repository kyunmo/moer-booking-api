package io.moer.booking.domain.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCreateRequest {

    // ========================================
    // Case 1: 관리자가 기존 고객으로 예약 등록
    // ========================================
    private Long customerId;  // 기존 고객 ID (있으면 사용)

    // ========================================
    // Case 2: 고객 자동 생성 (온라인 예약 or 신규 고객)
    // ========================================
    @NotBlank(message = "고객 이름은 필수입니다")
    private String customerName;  // 고객 이름 (자동 생성용)

    @NotBlank(message = "고객 전화번호는 필수입니다")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$",
            message = "전화번호 형식이 올바르지 않습니다 (예: 010-1234-5678)")
    private String customerPhone;  // 고객 전화번호 (자동 생성용)

    // ========================================
    // 예약 정보
    // ========================================
    private Long staffId;  // NULL 가능 (상관없음)

    @NotEmpty(message = "서비스는 최소 1개 이상 선택해야 합니다")
    private List<Long> serviceIds;

    @NotNull(message = "예약 날짜는 필수입니다")
    private LocalDate reservationDate;

    @NotNull(message = "시작 시간은 필수입니다")
    private LocalTime startTime;

    private String customerRequest;  // 고객 요청사항
}