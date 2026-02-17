package io.moer.booking.domain.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 고객 엔티티
 * DB 테이블: customers
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    private Long id;
    private Long businessId;
    private Long userId;        // 로그인 고객 사용자 ID
    private String name;
    private String phone;
    private String email;
    private LocalDate birthDate;
    private String gender;

    private Integer visitCount;
    private Integer totalSpent;
    private LocalDate lastVisitDate;

    /**
     * 태그 (콤마 구분 TEXT)
     * DB: TEXT
     * 예: "VIP,단골,신규"
     */
    private String tags;

    private String memo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========================================
    // 헬퍼 메서드 - 고객 타입 판별
    // ========================================

    /**
     * VIP 고객 여부 (10회 이상 방문)
     */
    public boolean isVip() {
        return visitCount != null && visitCount >= 10;
    }

    /**
     * 신규 고객 여부 (1회 방문)
     */
    public boolean isNew() {
        return visitCount != null && visitCount == 1;
    }

    /**
     * 단골 고객 여부 (3회 이상 방문)
     */
    public boolean isRegular() {
        return visitCount != null && visitCount >= 3;
    }

    // ========================================
    // 헬퍼 메서드 - tags 변환
    // ========================================

    /**
     * tags String → List<String> 변환
     * "VIP,단골,신규" → ["VIP", "단골", "신규"]
     */
    public List<String> getTagList() {
        if (tags == null || tags.trim().isEmpty()) {
            return List.of();
        }

        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * List<String> → tags String 변환
     * ["VIP", "단골", "신규"] → "VIP,단골,신규"
     */
    public static String tagsToString(List<String> tagList) {
        if (tagList == null || tagList.isEmpty()) {
            return null;
        }

        return String.join(",", tagList);
    }
}