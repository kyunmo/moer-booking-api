package io.moer.booking.domain.service.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.service.Service;
import io.moer.booking.domain.service.category.dto.SortOrderUpdateRequest;
import io.moer.booking.domain.service.category.repository.ServiceCategoryRepository;
import io.moer.booking.domain.service.dto.ServiceCreateRequest;
import io.moer.booking.domain.service.dto.ServiceResponse;
import io.moer.booking.domain.service.dto.ServiceSearchCondition;
import io.moer.booking.domain.service.dto.ServiceUpdateRequest;
import io.moer.booking.domain.service.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 서비스 관리 Service
 */
@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final BusinessRepository businessRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final io.moer.booking.domain.business.service.OnboardingService onboardingService;

    /**
     * 서비스 생성
     */
    @Transactional
    public ServiceResponse createService(Long businessId, ServiceCreateRequest request) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        // categoryId 유효성 검증 (null 허용)
        if (request.getCategoryId() != null) {
            validateCategoryExists(businessId, request.getCategoryId());
        }

        // List<Long> → String 변환
        String staffIdsString = Service.staffIdsToString(request.getStaffIds());

        Service service = Service.builder()
                .businessId(businessId)
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .description(request.getDescription())
                .duration(request.getDuration())
                .price(request.getPrice())
                .staffIds(staffIdsString)
                .sortOrder(0)
                .isActive("Y")
                .build();

        serviceRepository.save(service);

        log.info("Service created: id={}, businessId={}, name={}",
                service.getId(), businessId, service.getName());

        // 온보딩 스텝 자동 완료
        onboardingService.markStepComplete(businessId, "service");

        // JOIN된 정보를 포함하여 반환하기 위해 다시 조회
        return getService(businessId, service.getId());
    }

    /**
     * 서비스 단건 조회
     */
    public ServiceResponse getService(Long businessId, Long serviceId) {
        if (!serviceRepository.existsByBusinessIdAndId(businessId, serviceId)) {
            throw new EntityNotFoundException(ErrorCode.SERVICE_NOT_FOUND);
        }

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SERVICE_NOT_FOUND));

        return ServiceResponse.from(service);
    }

    /**
     * Business의 전체 서비스 조회
     */
    public List<ServiceResponse> getServicesByBusiness(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return serviceRepository.findByBusinessId(businessId).stream()
                .map(ServiceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Business의 활성 서비스만 조회
     */
    public List<ServiceResponse> getActiveServicesByBusiness(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return serviceRepository.findActiveByBusinessId(businessId).stream()
                .map(ServiceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리별 서비스 조회
     */
    public List<ServiceResponse> getServicesByCategory(Long businessId, Long categoryId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return serviceRepository.findByBusinessIdAndCategoryId(businessId, categoryId).stream()
                .map(ServiceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 조건별 서비스 검색
     */
    public List<ServiceResponse> searchServices(ServiceSearchCondition condition) {
        if (!businessRepository.existsById(condition.getBusinessId())) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return serviceRepository.search(condition).stream()
                .map(ServiceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 서비스 수정
     */
    @Transactional
    public ServiceResponse updateService(Long businessId, Long serviceId, ServiceUpdateRequest request) {
        if (!serviceRepository.existsByBusinessIdAndId(businessId, serviceId)) {
            throw new EntityNotFoundException(ErrorCode.SERVICE_NOT_FOUND);
        }

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SERVICE_NOT_FOUND));

        // categoryId 유효성 검증 (null 허용)
        Long newCategoryId = request.getCategoryId() != null ? request.getCategoryId() : service.getCategoryId();
        if (request.getCategoryId() != null) {
            validateCategoryExists(businessId, request.getCategoryId());
        }

        // List<Long> → String 변환
        String staffIdsString = request.getStaffIds() != null
                ? Service.staffIdsToString(request.getStaffIds())
                : service.getStaffIds();

        Service updatedService = Service.builder()
                .id(service.getId())
                .businessId(service.getBusinessId())
                .categoryId(newCategoryId)
                .name(request.getName() != null ? request.getName() : service.getName())
                .description(request.getDescription() != null ? request.getDescription() : service.getDescription())
                .duration(request.getDuration() != null ? request.getDuration() : service.getDuration())
                .price(request.getPrice() != null ? request.getPrice() : service.getPrice())
                .staffIds(staffIdsString)
                .sortOrder(service.getSortOrder())
                .isActive(service.getIsActive())
                .createdAt(service.getCreatedAt())
                .build();

        serviceRepository.update(updatedService);

        log.info("Service updated: id={}, businessId={}", serviceId, businessId);

        return getService(businessId, serviceId);
    }

    /**
     * 서비스 활성/비활성 토글
     */
    @Transactional
    public ServiceResponse toggleServiceActive(Long businessId, Long serviceId) {
        if (!serviceRepository.existsByBusinessIdAndId(businessId, serviceId)) {
            throw new EntityNotFoundException(ErrorCode.SERVICE_NOT_FOUND);
        }

        serviceRepository.toggleActive(serviceId);

        log.info("Service active toggled: id={}, businessId={}", serviceId, businessId);

        return getService(businessId, serviceId);
    }

    /**
     * 서비스 삭제
     */
    @Transactional
    public void deleteService(Long businessId, Long serviceId) {
        if (!serviceRepository.existsByBusinessIdAndId(businessId, serviceId)) {
            throw new EntityNotFoundException(ErrorCode.SERVICE_NOT_FOUND);
        }

        serviceRepository.delete(serviceId);

        log.info("Service deleted: id={}, businessId={}", serviceId, businessId);
    }

    /**
     * 서비스 정렬 순서 일괄 변경
     */
    @Transactional
    public List<ServiceResponse> updateServiceSortOrder(Long businessId, SortOrderUpdateRequest request) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        for (SortOrderUpdateRequest.SortOrderItem item : request.getItems()) {
            if (!serviceRepository.existsByBusinessIdAndId(businessId, item.getId())) {
                throw new BusinessException(ErrorCode.SERVICE_CATEGORY_SORT_ORDER_INVALID,
                        "존재하지 않는 서비스 ID: " + item.getId());
            }
            serviceRepository.updateSortOrder(item.getId(), item.getSortOrder());
        }

        log.info("Service sort order updated: businessId={}, count={}",
                businessId, request.getItems().size());

        return getServicesByBusiness(businessId);
    }

    /**
     * categoryId가 해당 매장에 존재하는지 검증
     */
    private void validateCategoryExists(Long businessId, Long categoryId) {
        if (!serviceCategoryRepository.existsByBusinessIdAndId(businessId, categoryId)) {
            throw new EntityNotFoundException(ErrorCode.SERVICE_CATEGORY_NOT_FOUND);
        }
    }
}
