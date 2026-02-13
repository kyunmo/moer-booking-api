package io.moer.booking.domain.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 서비스 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCreateRequest {

    private Long categoryId;

    @NotBlank(message = "서비스명은 필수입니다")
    @Size(min = 2, max = 100, message = "서비스명은 2~100자 사이여야 합니다")
    private String name;

    private String description;

    @NotNull(message = "가격은 필수입니다")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다")
    private Integer price;

    @NotNull(message = "소요 시간은 필수입니다")
    @Min(value = 1, message = "소요 시간은 1분 이상이어야 합니다")
    private Integer duration;

    /**
     * 담당 가능 직원 ID 목록
     */
    private List<Long> staffIds;
}
