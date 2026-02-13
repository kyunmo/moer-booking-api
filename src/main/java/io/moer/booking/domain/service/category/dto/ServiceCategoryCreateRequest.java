package io.moer.booking.domain.service.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서비스 카테고리 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCategoryCreateRequest {

    @NotBlank(message = "카테고리명은 필수입니다")
    @Size(max = 50, message = "카테고리명은 50자 이내여야 합니다")
    private String name;

    @Size(max = 200, message = "설명은 200자 이내여야 합니다")
    private String description;
}
