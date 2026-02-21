package io.moer.booking.domain.inquiry.dto;

import io.moer.booking.domain.inquiry.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 문의 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "문의하기 요청")
public class InquiryCreateRequest {

    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다")
    @Schema(description = "문의자 이름", example = "홍길동")
    private String name;

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    @Size(max = 100, message = "이메일은 100자 이하여야 합니다")
    @Schema(description = "문의자 이메일", example = "example@email.com")
    private String email;

    @Size(max = 20, message = "전화번호는 20자 이하여야 합니다")
    @Schema(description = "문의자 전화번호 (선택)", example = "010-1234-5678")
    private String phone;

    @NotNull(message = "문의 유형은 필수입니다")
    @Schema(description = "문의 유형", example = "GENERAL")
    private InquiryType type;

    @NotBlank(message = "문의 내용은 필수입니다")
    @Size(max = 5000, message = "문의 내용은 5000자 이하여야 합니다")
    @Schema(description = "문의 내용", example = "서비스 이용에 대해 문의드립니다.")
    private String content;
}
