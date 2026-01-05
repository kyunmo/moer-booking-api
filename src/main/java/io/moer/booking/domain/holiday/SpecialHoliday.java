package io.moer.booking.domain.holiday;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 특별 휴무일 엔티티
 * DB 테이블: special_holidays
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialHoliday {

    private Long id;
    private Long businessId;

    /**
     * 휴무일 이름
     * 예: 설날, 추석, 임시 휴무
     */
    private String name;

    /**
     * 휴무 날짜
     */
    private LocalDate date;

    /**
     * 휴무일 유형
     * REGULAR: 정기 휴무
     * TEMPORARY: 임시 휴무
     * NATIONAL: 공휴일
     */
    private String type;

    /**
     * 휴무 사유 (상세 설명)
     */
    private String reason;

    private LocalDateTime createdAt;
}