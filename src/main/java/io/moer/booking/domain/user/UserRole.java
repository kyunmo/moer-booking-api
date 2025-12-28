package io.moer.booking.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    ADMIN("시스템 관리자"),
    OWNER("매장 사장님"),
    STAFF("직원");

    private final String description;
}