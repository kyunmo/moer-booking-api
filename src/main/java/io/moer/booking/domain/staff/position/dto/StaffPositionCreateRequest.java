package io.moer.booking.domain.staff.position.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 직급 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StaffPositionCreateRequest {

    @NotBlank(message = "직급명은 필수입니다")
    @Size(max = 50, message = "직급명은 50자 이내여야 합니다")
    private String name;

    @Size(max = 200, message = "설명은 200자 이내여야 합니다")
    private String description;
}
