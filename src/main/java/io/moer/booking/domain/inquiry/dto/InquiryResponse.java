package io.moer.booking.domain.inquiry.dto;

import io.moer.booking.domain.inquiry.Inquiry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 문의 응답 DTO
 * 접수 완료 후 간단한 응답만 반환
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "문의하기 응답")
public class InquiryResponse {

    @Schema(description = "문의 ID", example = "1")
    private Long id;

    @Schema(description = "접수 메시지", example = "문의가 접수되었습니다.")
    private String message;

    /**
     * Entity -> DTO 변환
     */
    public static InquiryResponse from(Inquiry inquiry) {
        return InquiryResponse.builder()
                .id(inquiry.getId())
                .message("문의가 접수되었습니다.")
                .build();
    }
}
