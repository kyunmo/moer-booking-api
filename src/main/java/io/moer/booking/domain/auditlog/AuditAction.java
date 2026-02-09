package io.moer.booking.domain.auditlog;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 감사 로그 액션 타입
 */
@Getter
@RequiredArgsConstructor
public enum AuditAction {
    // Business
    BUSINESS_CREATED("매장 생성"),
    BUSINESS_UPDATED("매장 수정"),
    BUSINESS_DELETED("매장 삭제"),
    BUSINESS_STATUS_CHANGED("매장 상태 변경"),

    // User
    USER_CREATED("사용자 생성"),
    USER_ROLE_CHANGED("사용자 역할 변경"),
    USER_STATUS_CHANGED("사용자 상태 변경"),
    USER_DELETED("사용자 삭제"),

    // System
    SYSTEM_BACKUP("시스템 백업"),
    SYSTEM_RESTORE("시스템 복원"),
    SYSTEM_CONFIG_CHANGED("시스템 설정 변경");

    private final String description;
}
