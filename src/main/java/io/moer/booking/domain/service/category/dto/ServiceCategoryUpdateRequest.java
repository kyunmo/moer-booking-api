package io.moer.booking.domain.service.category.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서비스 카테고리 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCategoryUpdateRequest {

    @Size(max = 50, message = "카테고리명은 50자 이내여야 합니다")
    private String name;

    @Size(max = 200, message = "설명은 200자 이내여야 합니다")
    private String description;
}
