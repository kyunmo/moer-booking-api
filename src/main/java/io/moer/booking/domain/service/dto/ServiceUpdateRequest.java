package io.moer.booking.domain.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceUpdateRequest {

    @Size(max = 50, message = "카테고리는 50자 이내여야 합니다")
    private String category;

    @Size(min = 2, max = 100, message = "서비스명은 2~100자 사이여야 합니다")
    private String name;

    private String description;

    @Min(value = 0, message = "가격은 0원 이상이어야 합니다")
    private Integer price;

    @Min(value = 1, message = "소요 시간은 1분 이상이어야 합니다")
    private Integer duration;

    private String imageUrl;
    private Map<String, Object> options;
    private List<Long> availableStaffIds;
    private Integer displayOrder;
}