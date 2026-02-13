package io.moer.booking.domain.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 서비스(메뉴) 엔티티
 * DB 테이블: services
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service {
    private Long id;
    private Long businessId;
    private Long categoryId;
    private String categoryName;  // JOIN용 (service_categories.name)
    private String name;
    private String description;
    private Integer duration;
    private Integer price;

    /**
     * 담당 가능 직원 ID 목록 (콤마 구분 TEXT)
     * DB: TEXT
     * 예: "1,2,3"
     */
    private String staffIds;

    /**
     * 활성 여부 (Y/N)
     * DB: CHAR(1)
     */
    private Integer sortOrder;

    private String isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========================================
    // 헬퍼 메서드 - 활성 여부
    // ========================================

    public boolean isActive() {
        return "Y".equals(this.isActive);
    }

    // ========================================
    // 헬퍼 메서드 - staffIds 변환
    // ========================================

    /**
     * staffIds String → List<Long> 변환
     * "1,2,3" → [1L, 2L, 3L]
     */
    public List<Long> getStaffIdList() {
        if (staffIds == null || staffIds.trim().isEmpty()) {
            return List.of();
        }

        return Arrays.stream(staffIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    /**
     * List<Long> → staffIds String 변환
     * [1L, 2L, 3L] → "1,2,3"
     */
    public static String staffIdsToString(List<Long> staffIdList) {
        if (staffIdList == null || staffIdList.isEmpty()) {
            return null;
        }

        return staffIdList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}