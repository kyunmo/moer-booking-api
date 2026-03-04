package io.moer.booking.domain.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "고객 메모 생성/수정 요청")
public class CustomerNoteRequest {

    @NotBlank(message = "메모 내용은 필수입니다")
    @Size(max = 2000, message = "메모는 2000자 이내로 입력해주세요")
    @Schema(description = "메모 내용", example = "알레르기 주의 - 견과류")
    private String content;

    @Schema(description = "비공개 여부", example = "false")
    private Boolean isPrivate;
}
