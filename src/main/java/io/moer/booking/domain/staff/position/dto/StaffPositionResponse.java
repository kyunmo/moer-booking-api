package io.moer.booking.domain.staff.position.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.staff.position.StaffPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 직급 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class StaffPositionResponse {

    private Long id;
    private Long businessId;
    private String name;
    private String description;
    private Integer sortOrder;
    private Integer staffCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Entity -> DTO 변환
     */
    public static StaffPositionResponse from(StaffPosition position) {
        return StaffPositionResponse.builder()
                .id(position.getId())
                .businessId(position.getBusinessId())
                .name(position.getName())
                .description(position.getDescription())
                .sortOrder(position.getSortOrder())
                .createdAt(position.getCreatedAt())
                .updatedAt(position.getUpdatedAt())
                .build();
    }

    /**
     * Entity -> DTO 변환 (staffCount 포함)
     */
    public static StaffPositionResponse from(StaffPosition position, int staffCount) {
        return StaffPositionResponse.builder()
                .id(position.getId())
                .businessId(position.getBusinessId())
                .name(position.getName())
                .description(position.getDescription())
                .sortOrder(position.getSortOrder())
                .staffCount(staffCount)
                .createdAt(position.getCreatedAt())
                .updatedAt(position.getUpdatedAt())
                .build();
    }
}
