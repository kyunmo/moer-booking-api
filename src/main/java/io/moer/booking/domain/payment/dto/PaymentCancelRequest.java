package io.moer.booking.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 취소 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "결제 취소 요청")
public class PaymentCancelRequest {

    @NotBlank(message = "취소 사유는 필수입니다")
    @Size(max = 200, message = "취소 사유는 200자 이내여야 합니다")
    @Schema(description = "취소 사유", example = "고객 요청에 의한 취소")
    private String reason;
}
