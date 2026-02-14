package io.moer.booking.domain.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 직원 엔티티
 * DB 테이블: staffs
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff {
    private Long id;
    private Long businessId;
    private String name;
    private String position;
    private Long positionId;
    private String phone;
    private String email;
    private String specialty;
    private Integer careerYears;
    private String profileImageUrl;
    private String introduction;

    /**
     * 활성 여부 (Y/N)
     * DB: CHAR(1)
     */
    private String isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 활성 상태 확인
     */
    public boolean isActive() {
        return "Y".equals(this.isActive);
    }
}