package io.moer.booking.domain.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.service.Service;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 서비스 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class ServiceResponse {

    private Long id;
    private Long businessId;

    private String category;
    private String name;
    private String description;

    private Integer price;
    private Integer duration;

    /**
     * 담당 가능 직원 ID 목록
     * DB의 콤마 구분 문자열을 List로 변환
     */
    private List<Long> staffIds;

    /**
     * 활성 여부
     */
    private Boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static ServiceResponse from(Service service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .businessId(service.getBusinessId())
                .category(service.getCategory())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .duration(service.getDuration())
                .staffIds(service.getStaffIdList())  // String → List 변환
                .isActive(service.isActive())  // "Y" → true 변환
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .build();
    }
}