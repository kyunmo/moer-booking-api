package io.moer.booking.domain.auditlog;

import io.moer.booking.domain.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 감사 로그 엔티티
 * DB 테이블: audit_logs
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    private Long id;

    // 액션 수행자 정보
    private Long userId;
    private String userEmail;
    private UserRole userRole;

    // 액션 정보
    private String action;          // BUSINESS_CREATED, USER_ROLE_CHANGED 등
    private String entityType;      // Business, User, Reservation 등
    private Long entityId;

    // 상세 정보
    private String description;
    private Map<String, Object> metadata;  // JSONB - 변경 전/후 값

    // 요청 정보
    private String ipAddress;
    private String userAgent;

    private LocalDateTime createdAt;
}
