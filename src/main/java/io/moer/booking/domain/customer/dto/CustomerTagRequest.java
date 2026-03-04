package io.moer.booking.domain.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "고객 태그 수정 요청")
public class CustomerTagRequest {

    @NotNull(message = "태그 목록은 필수입니다")
    @Schema(description = "태그 목록", example = "[\"VIP\", \"단골\"]")
    private List<String> tags;
}
