package io.moer.booking.domain.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 고객용 예약 생성 요청 DTO (Public API)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "고객용 예약 생성 요청")
public class PublicReservationCreateRequest {

    @NotEmpty(message = "서비스는 최소 1개 이상 선택해야 합니다")
    @Schema(description = "서비스 ID 목록", example = "[1, 2]")
    private List<Long> serviceIds;

    @Schema(description = "담당 스태프 ID (미지정 시 자동 배정)", example = "1")
    private Long staffId;

    @NotNull(message = "예약 날짜는 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "예약 날짜", example = "2026-02-20")
    private LocalDate reservationDate;

    @NotNull(message = "시작 시간은 필수입니다")
    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "시작 시간", example = "14:00")
    private LocalTime startTime;

    @NotBlank(message = "고객 이름은 필수입니다")
    @Schema(description = "고객 이름", example = "홍길동")
    private String customerName;

    @NotBlank(message = "고객 전화번호는 필수입니다")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$",
            message = "전화번호 형식이 올바르지 않습니다 (예: 010-1234-5678)")
    @Schema(description = "고객 전화번호", example = "010-1234-5678")
    private String customerPhone;

    @Schema(description = "고객 이메일 (선택)", example = "hong@example.com")
    private String customerEmail;

    @Schema(description = "고객 요청사항 (선택)", example = "조용한 자리 부탁드립니다")
    private String customerRequest;
}
