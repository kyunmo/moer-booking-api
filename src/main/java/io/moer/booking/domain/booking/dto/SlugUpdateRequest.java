package io.moer.booking.domain.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 슬러그 변경 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SlugUpdateRequest {

    @NotBlank(message = "슬러그는 필수입니다")
    @Size(min = 3, max = 50, message = "슬러그는 3~50자여야 합니다")
    @Pattern(
            regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$",
            message = "슬러그는 소문자, 숫자, 하이픈만 사용 가능하며, 시작과 끝은 소문자 또는 숫자여야 합니다"
    )
    private String slug;
}
