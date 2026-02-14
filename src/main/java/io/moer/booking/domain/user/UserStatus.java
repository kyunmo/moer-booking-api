package io.moer.booking.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
    ACTIVE("활성"),
    INACTIVE("휴면"),
    SUSPENDED("정지"),
    DELETED("탈퇴");

    private final String description;
}