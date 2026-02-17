package io.moer.booking.domain.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객용 예약 취소 요청 DTO (Public API)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "예약 취소 요청")
public class PublicReservationCancelRequest {

    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$",
            message = "전화번호 형식이 올바르지 않습니다 (예: 010-1234-5678)")
    @Schema(description = "예약 시 입력한 전화번호 (본인 확인용)", example = "010-1234-5678")
    private String phone;

    @Schema(description = "취소 사유 (선택)", example = "개인 사정으로 취소합니다")
    private String reason;
}
