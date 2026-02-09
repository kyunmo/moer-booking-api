package io.moer.booking.domain.auditlog.dto;

import io.moer.booking.domain.auditlog.AuditLog;
import io.moer.booking.domain.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private UserRole userRole;
    private String action;
    private String entityType;
    private Long entityId;
    private String description;
    private Map<String, Object> metadata;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;

    public static AuditLogResponse from(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUserId())
                .userEmail(auditLog.getUserEmail())
                .userRole(auditLog.getUserRole())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .description(auditLog.getDescription())
                .metadata(auditLog.getMetadata())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
