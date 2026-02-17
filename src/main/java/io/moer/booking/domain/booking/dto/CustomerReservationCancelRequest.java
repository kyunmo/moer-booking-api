package io.moer.booking.domain.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 고객용 예약 취소 요청 DTO
 * 전화번호 확인이 불필요합니다 (userId 기반 본인 인증 완료).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "고객 예약 취소 요청")
public class CustomerReservationCancelRequest {

    @Schema(description = "취소 사유 (선택)", example = "일정이 변경되었습니다")
    private String reason;
}
