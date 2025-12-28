package io.moer.booking.domain.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    private Long id;
    private Long businessId;

    // 기본 정보
    private String name;
    private String phone;
    private String email;

    // 통계
    private Integer visitCount;
    private Integer totalSpent;
    private LocalDate lastVisitDate;

    // 태그
    private List<String> tags;

    // 관리자 메모
    private String adminMemo;

    // 카카오톡
    private String kakaoUserKey;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 방문 횟수 증가
     */
    public void incrementVisitCount() {
        this.visitCount = (this.visitCount != null ? this.visitCount : 0) + 1;
    }

    /**
     * 결제 금액 추가
     */
    public void addSpent(int amount) {
        this.totalSpent = (this.totalSpent != null ? this.totalSpent : 0) + amount;
    }

    /**
     * 최근 방문일 업데이트
     */
    public void updateLastVisitDate(LocalDate date) {
        this.lastVisitDate = date;
    }

    /**
     * VIP 고객 여부 확인 (10회 이상 방문)
     */
    public boolean isVip() {
        return visitCount != null && visitCount >= 10;
    }

    /**
     * 신규 고객 여부 확인 (1회 방문)
     */
    public boolean isNew() {
        return visitCount != null && visitCount == 1;
    }

    /**
     * 단골 고객 여부 확인 (3회 이상 방문)
     */
    public boolean isRegular() {
        return visitCount != null && visitCount >= 3;
    }
}