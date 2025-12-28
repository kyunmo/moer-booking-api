package io.moer.booking.domain.staff.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.staff.Staff;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class StaffResponse {

    private Long id;
    private Long businessId;
    private Long userId;
    private String name;
    private String nickname;
    private String phone;
    private String email;

    // 프로필
    private String profileImageUrl;
    private String introduction;
    private Integer careerYears;
    private List<String> specialties;

    // 근무 정보
    private Map<String, Object> workSchedule;
    private Boolean isActive;
    private Integer displayOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public static StaffResponse from(Staff staff) {
        return StaffResponse.builder()
                .id(staff.getId())
                .businessId(staff.getBusinessId())
                .userId(staff.getUserId())
                .name(staff.getName())
                .nickname(staff.getNickname())
                .phone(staff.getPhone())
                .email(staff.getEmail())
                .profileImageUrl(staff.getProfileImageUrl())
                .introduction(staff.getIntroduction())
                .careerYears(staff.getCareerYears())
                .specialties(staff.getSpecialties())
                .workSchedule(staff.getWorkSchedule())
                .isActive(staff.getIsActive())
                .displayOrder(staff.getDisplayOrder())
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .build();
    }
}