package io.moer.booking.domain.help;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 도움말 카테고리
 */
@Getter
@RequiredArgsConstructor
public enum HelpCategory {

    RESERVATION("예약 관리", "mdi-calendar"),
    STAFF("직원 관리", "mdi-account-group"),
    SERVICE("서비스 관리", "mdi-content-cut"),
    PAYMENT("결제", "mdi-credit-card"),
    STATISTICS("통계", "mdi-chart-bar");

    private final String name;
    private final String icon;

    /**
     * 카테고리 ID 문자열에서 HelpCategory를 반환.
     * 유효하지 않은 값이면 null 반환.
     */
    public static HelpCategory fromId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return valueOf(id.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
