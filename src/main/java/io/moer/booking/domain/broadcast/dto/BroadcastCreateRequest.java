package io.moer.booking.domain.broadcast.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공지 발송 요청")
public class BroadcastCreateRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    @Schema(description = "공지 제목", example = "서비스 점검 안내")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    @Schema(description = "공지 내용")
    private String content;

    @Schema(description = "발송 대상 (ALL, PAID, TRIAL, FREE)", example = "ALL")
    private String targetType;

    @Schema(description = "우선순위 (LOW, NORMAL, HIGH, URGENT)", example = "NORMAL")
    private String priority;
}
