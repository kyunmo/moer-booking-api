package io.moer.booking.domain.auditlog.dto;

import io.moer.booking.domain.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogCreateRequest {
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
}
