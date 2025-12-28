package io.moer.booking.domain.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSearchCondition {

    private Long businessId;
    private String name;  // 이름 검색 (LIKE)
    private String phone;  // 전화번호 검색 (LIKE)
    private String tag;  // 특정 태그 검색 (VIP, 단골, 신규)
    private Integer minVisitCount;  // 최소 방문 횟수
    private String sortBy;  // 정렬 기준 (visit_count, total_spent, last_visit_date)
    private String sortOrder;  // 정렬 방향 (asc, desc)
}