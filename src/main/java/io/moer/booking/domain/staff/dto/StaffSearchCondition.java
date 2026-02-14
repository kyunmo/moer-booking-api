package io.moer.booking.domain.staff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffSearchCondition {

    private Long businessId;
    private String name;          // 이름 검색 (LIKE)
    private Long positionId;      // 직급 ID (exact)
    private String specialty;     // 전문분야 검색 (LIKE)
    private Boolean isActive;     // 활성 여부
    private Integer minCareerYears; // 최소 경력 (>=)
    private String sortBy;        // 정렬 기준 (name, position, career_years, created_at)
    private String sortOrder;     // 정렬 방향 (asc, desc)
}
