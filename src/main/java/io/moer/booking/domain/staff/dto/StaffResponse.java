package io.moer.booking.domain.staff.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.staff.Staff;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 직원 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffResponse {
    private Long id;
    private Long businessId;
    private String name;
    private String position;
    private Long positionId;
    private String positionName;
    private String phone;
    private String email;
    private String specialty;
    private Integer careerYears;
    private String profileImageUrl;
    private String introduction;

    /**
     * 활성 여부
     * DB의 Y/N을 boolean으로 변환
     */
    private Boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static StaffResponse from(Staff staff) {
        return StaffResponse.builder()
                .id(staff.getId())
                .businessId(staff.getBusinessId())
                .name(staff.getName())
                .position(staff.getPosition())
                .positionId(staff.getPositionId())
                .phone(staff.getPhone())
                .email(staff.getEmail())
                .specialty(staff.getSpecialty())
                .careerYears(staff.getCareerYears())
                .profileImageUrl(staff.getProfileImageUrl())
                .introduction(staff.getIntroduction())
                .isActive("Y".equals(staff.getIsActive()))  // Y/N → boolean
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .build();
    }

    /**
     * Entity → DTO 변환 (positionName 포함)
     */
    public static StaffResponse from(Staff staff, String positionName) {
        return StaffResponse.builder()
                .id(staff.getId())
                .businessId(staff.getBusinessId())
                .name(staff.getName())
                .position(staff.getPosition())
                .positionId(staff.getPositionId())
                .positionName(positionName)
                .phone(staff.getPhone())
                .email(staff.getEmail())
                .specialty(staff.getSpecialty())
                .careerYears(staff.getCareerYears())
                .profileImageUrl(staff.getProfileImageUrl())
                .introduction(staff.getIntroduction())
                .isActive("Y".equals(staff.getIsActive()))
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .build();
    }
}
