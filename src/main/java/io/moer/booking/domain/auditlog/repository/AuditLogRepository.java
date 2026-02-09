package io.moer.booking.domain.auditlog.repository;

import io.moer.booking.domain.auditlog.AuditLog;
import io.moer.booking.domain.auditlog.dto.AuditLogSearchCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface AuditLogRepository {

    /**
     * 감사 로그 저장
     */
    void save(AuditLog auditLog);

    /**
     * ID로 조회
     */
    Optional<AuditLog> findById(Long id);

    /**
     * 검색 조건으로 조회 (페이징)
     */
    List<AuditLog> findByCondition(
            @Param("condition") AuditLogSearchCondition condition,
            @Param("offset") int offset,
            @Param("size") int size
    );

    /**
     * 검색 조건으로 개수 조회
     */
    int countByCondition(@Param("condition") AuditLogSearchCondition condition);
}
