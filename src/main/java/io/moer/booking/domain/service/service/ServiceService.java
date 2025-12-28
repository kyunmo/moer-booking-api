package io.moer.booking.domain.service.service;

import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.service.Service;
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

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final BusinessRepository businessRepository;

    /**
     * Service 생성
     */
    @Transactional
    public ServiceResponse createService(Long businessId, ServiceCreateRequest request) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        // Service 엔티티 생성
        Service service = Service.builder()
                .businessId(businessId)
                .category(request.getCategory())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .duration(request.getDuration())
                .imageUrl(request.getImageUrl())
                .options(request.getOptions())
                .availableStaffIds(request.getAvailableStaffIds())
                .isActive(true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();

        // 저장
        serviceRepository.save(service);

        log.info("Service created: id={}, businessId={}, name={}",
                service.getId(), businessId, service.getName());

        return ServiceResponse.from(service);
    }

    /**
     * Service 단건 조회
     */
    public ServiceResponse getService(Long businessId, Long serviceId) {
        // Business의 Service인지 확인
        if (!serviceRepository.existsByBusinessIdAndId(businessId, serviceId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        return ServiceResponse.from(service);
    }

    /**
     * Business의 Service 목록 조회
     */
    public List<ServiceResponse> getServicesByBusiness(Long businessId) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return serviceRepository.findByBusinessId(businessId).stream()
                .map(ServiceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Business의 활성 Service 목록 조회
     */
    public List<ServiceResponse> getActiveServicesByBusiness(Long businessId) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return serviceRepository.findByBusinessIdAndActive(businessId, true).stream()
                .map(ServiceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리별 Service 목록 조회
     */
    public List<ServiceResponse> getServicesByCategory(Long businessId, String category) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return serviceRepository.findByBusinessIdAndCategory(businessId, category).stream()
                .map(ServiceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 조건별 Service 검색
     */
    public List<ServiceResponse> searchServices(ServiceSearchCondition condition) {
        // Business 존재 확인
        if (!businessRepository.existsById(condition.getBusinessId())) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return serviceRepository.findByCondition(condition).stream()
                .map(ServiceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Service 수정
     */
    @Transactional
    public ServiceResponse updateService(Long businessId, Long serviceId, ServiceUpdateRequest request) {
        // Business의 Service인지 확인
        if (!serviceRepository.existsByBusinessIdAndId(businessId, serviceId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        // 수정할 필드만 업데이트
        Service updatedService = Service.builder()
                .id(service.getId())
                .category(request.getCategory() != null ? request.getCategory() : service.getCategory())
                .name(request.getName() != null ? request.getName() : service.getName())
                .description(request.getDescription() != null ? request.getDescription() : service.getDescription())
                .price(request.getPrice() != null ? request.getPrice() : service.getPrice())
                .duration(request.getDuration() != null ? request.getDuration() : service.getDuration())
                .imageUrl(request.getImageUrl() != null ? request.getImageUrl() : service.getImageUrl())
                .options(request.getOptions() != null ? request.getOptions() : service.getOptions())
                .availableStaffIds(request.getAvailableStaffIds() != null ?
                        request.getAvailableStaffIds() : service.getAvailableStaffIds())
                .displayOrder(request.getDisplayOrder() != null ?
                        request.getDisplayOrder() : service.getDisplayOrder())
                .build();

        serviceRepository.update(updatedService);

        log.info("Service updated: id={}, businessId={}", serviceId, businessId);

        return getService(businessId, serviceId);
    }

    /**
     * Service 활성/비활성 전환
     */
    @Transactional
    public ServiceResponse toggleServiceActive(Long businessId, Long serviceId) {
        // Business의 Service인지 확인
        if (!serviceRepository.existsByBusinessIdAndId(businessId, serviceId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        Service updated = Service.builder()
                .id(service.getId())
                .isActive(!service.getIsActive())
                .build();

        serviceRepository.update(updated);

        log.info("Service active toggled: id={}, isActive={}", serviceId, !service.getIsActive());

        return getService(businessId, serviceId);
    }

    /**
     * Service 삭제
     */
    @Transactional
    public void deleteService(Long businessId, Long serviceId) {
        // Business의 Service인지 확인
        if (!serviceRepository.existsByBusinessIdAndId(businessId, serviceId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        serviceRepository.delete(serviceId);

        log.info("Service deleted: id={}, businessId={}", serviceId, businessId);
    }
}