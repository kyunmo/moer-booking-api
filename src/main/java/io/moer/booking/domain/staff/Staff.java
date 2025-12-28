package io.moer.booking.domain.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Staff {

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

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 비즈니스 로직
    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}