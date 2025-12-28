package io.moer.booking.domain.staff.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StaffCreateRequest {

    private Long userId;  // 선택

    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 50, message = "이름은 2~50자 사이여야 합니다")
    private String name;

    private String nickname;
    private String phone;
    private String email;

    // 프로필
    private String profileImageUrl;
    private String introduction;

    @Min(value = 0, message = "경력은 0년 이상이어야 합니다")
    private Integer careerYears;

    private List<String> specialties;

    // 근무 정보
    private Map<String, Object> workSchedule;
    private Integer displayOrder;
}