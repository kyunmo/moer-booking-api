package io.moer.booking.domain.auditlog.service;

import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.auditlog.AuditLog;
import io.moer.booking.domain.auditlog.AuditAction;
import io.moer.booking.domain.auditlog.dto.AuditLogCreateRequest;
import io.moer.booking.domain.auditlog.dto.AuditLogResponse;
import io.moer.booking.domain.auditlog.dto.AuditLogSearchCondition;
import io.moer.booking.domain.auditlog.repository.AuditLogRepository;
import io.moer.booking.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * 감사 로그 기록
     */
    @Transactional
    public void log(AuditLogCreateRequest request) {
        AuditLog auditLog = AuditLog.builder()
                .userId(request.getUserId())
                .userEmail(request.getUserEmail())
                .userRole(request.getUserRole())
                .action(request.getAction())
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .description(request.getDescription())
                .metadata(request.getMetadata())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .build();

        auditLogRepository.save(auditLog);

        log.info("Audit log created: userId={}, action={}, entityType={}, entityId={}",
                request.getUserId(), request.getAction(), request.getEntityType(), request.getEntityId());
    }

    /**
     * 감사 로그 기록 (간편 메서드)
     */
    @Transactional
    public void log(User user, AuditAction action, String entityType, Long entityId, String description, Map<String, Object> metadata) {
        AuditLog auditLog = AuditLog.builder()
                .userId(user != null ? user.getId() : null)
                .userEmail(user != null ? user.getEmail() : "SYSTEM")
                .userRole(user != null ? user.getRole() : null)
                .action(action.name())
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .metadata(metadata)
                .build();

        auditLogRepository.save(auditLog);

        log.info("Audit log created: userId={}, action={}, entityType={}, entityId={}",
                user != null ? user.getId() : "SYSTEM", action, entityType, entityId);
    }

    /**
     * 감사 로그 조회 (ID)
     */
    public AuditLogResponse getLog(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.AUDIT_LOG_NOT_FOUND));

        return AuditLogResponse.from(auditLog);
    }

    /**
     * 감사 로그 목록 조회 (검색 조건, 페이징)
     */
    public PageResponse<AuditLogResponse> getLogs(
            AuditLogSearchCondition condition,
            int page,
            int size) {

        int offset = (page - 1) * size;

        List<AuditLog> logs = auditLogRepository.findByCondition(condition, offset, size);
        int totalElements = auditLogRepository.countByCondition(condition);

        List<AuditLogResponse> content = logs.stream()
                .map(AuditLogResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(content, page, size, totalElements);
    }
}
