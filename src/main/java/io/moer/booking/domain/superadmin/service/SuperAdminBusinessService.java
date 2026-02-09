package io.moer.booking.domain.superadmin.service;

import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.auditlog.AuditAction;
import io.moer.booking.domain.auditlog.dto.AuditLogCreateRequest;
import io.moer.booking.domain.auditlog.service.AuditLogService;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.BusinessSettings;
import io.moer.booking.domain.business.BusinessStatus;
import io.moer.booking.domain.business.dto.BusinessResponse;
import io.moer.booking.domain.business.dto.BusinessSearchCondition;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.business.repository.BusinessSettingsRepository;
import io.moer.booking.domain.superadmin.dto.BulkStatusUpdateRequest;
import io.moer.booking.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 슈퍼 관리자 - 매장 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuperAdminBusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessSettingsRepository businessSettingsRepository;
    private final AuditLogService auditLogService;

    /**
     * 전체 매장 조회 (페이징, 필터링)
     */
    public PageResponse<BusinessResponse> getAllBusinesses(
            BusinessSearchCondition condition,
            int page,
            int size) {

        int offset = (page - 1) * size;

        List<Business> businesses = businessRepository.findAll(condition);
        long totalElements = businessRepository.countAll(condition);

        List<BusinessResponse> content = businesses.stream()
                .map(business -> {
                    BusinessSettings settings = businessSettingsRepository
                            .findByBusinessId(business.getId())
                            .orElse(null);
                    return BusinessResponse.from(business, settings);
                })
                .collect(Collectors.toList());

        return PageResponse.of(content, page, size, totalElements);
    }

    /**
     * 매장 강제 삭제
     */
    @Transactional
    public void forceDeleteBusiness(Long id, boolean hard, User admin) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        // 감사 로그 기록
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("businessName", business.getName());
        metadata.put("ownerId", business.getOwnerId());
        metadata.put("hardDelete", hard);

        auditLogService.log(AuditLogCreateRequest.builder()
                .userId(admin.getId())
                .userEmail(admin.getEmail())
                .userRole(admin.getRole())
                .action(AuditAction.BUSINESS_DELETED.name())
                .entityType("Business")
                .entityId(id)
                .description(String.format("매장 강제 삭제 (하드 삭제: %s)", hard))
                .metadata(metadata)
                .build());

        // 삭제 수행
        if (hard) {
            // 하드 삭제: Settings도 함께 삭제
            businessSettingsRepository.deleteByBusinessId(id);
        }

        businessRepository.delete(id);

        log.info("Business force deleted by SUPER_ADMIN: id={}, adminId={}, hard={}",
                id, admin.getId(), hard);
    }

    /**
     * 매장 상태 일괄 변경
     */
    @Transactional
    public void bulkUpdateStatus(BulkStatusUpdateRequest request, User admin) {
        for (Long businessId : request.getBusinessIds()) {
            Business business = businessRepository.findById(businessId)
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

            Business updated = Business.builder()
                    .id(business.getId())
                    .ownerId(business.getOwnerId())
                    .businessType(business.getBusinessType())
                    .name(business.getName())
                    .phone(business.getPhone())
                    .address(business.getAddress())
                    .description(business.getDescription())
                    .businessHours(business.getBusinessHours())
                    .status(request.getStatus())
                    .createdAt(business.getCreatedAt())
                    .build();

            businessRepository.update(updated);

            // 감사 로그 기록
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("businessName", business.getName());
            metadata.put("oldStatus", business.getStatus().name());
            metadata.put("newStatus", request.getStatus().name());

            auditLogService.log(AuditLogCreateRequest.builder()
                    .userId(admin.getId())
                    .userEmail(admin.getEmail())
                    .userRole(admin.getRole())
                    .action(AuditAction.BUSINESS_STATUS_CHANGED.name())
                    .entityType("Business")
                    .entityId(businessId)
                    .description(String.format("매장 상태 일괄 변경: %s -> %s",
                            business.getStatus(), request.getStatus()))
                    .metadata(metadata)
                    .build());
        }

        log.info("Bulk business status update by SUPER_ADMIN: count={}, status={}, adminId={}",
                request.getBusinessIds().size(), request.getStatus(), admin.getId());
    }
}
