package io.moer.booking.domain.service;

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
public class Service {

    private Long id;
    private Long businessId;

    private String category;  // "컷", "펌", "염색", "1:1수업", "그룹수업"
    private String name;
    private String description;

    // 가격/시간
    private Integer price;
    private Integer duration;  // 소요 시간 (분)

    // 이미지
    private String imageUrl;

    // 옵션 (업종별로 다름)
    private Map<String, Object> options;

    // 가능한 직원 (NULL이면 모든 직원 가능)
    private List<Long> availableStaffIds;

    // 표시
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

    /**
     * 특정 Staff가 이 서비스를 제공할 수 있는지 확인
     */
    public boolean isAvailableForStaff(Long staffId) {
        // availableStaffIds가 null이면 모든 직원 가능
        if (availableStaffIds == null || availableStaffIds.isEmpty()) {
            return true;
        }
        return availableStaffIds.contains(staffId);
    }
}