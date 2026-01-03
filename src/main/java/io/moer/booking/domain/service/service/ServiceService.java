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

    /**
     * 서비스 생성
     */
    @Transactional
    public ServiceResponse createService(Long businessId, ServiceCreateRequest request) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        // List<Long> → String 변환
        String staffIdsString = Service.staffIdsToString(request.getStaffIds());

        Service service = Service.builder()
                .businessId(businessId)
                .category(request.getCategory())
                .name(request.getName())
                .description(request.getDescription())
                .duration(request.getDuration())
                .price(request.getPrice())
                .staffIds(staffIdsString)
                .isActive("Y")
                .build();

        serviceRepository.save(service);

        log.info("Service created: id={}, businessId={}, name={}",
                service.getId(), businessId, service.getName());

        return ServiceResponse.from(service);
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
    public List<ServiceResponse> getServicesByCategory(Long businessId, String category) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return serviceRepository.findByBusinessIdAndCategory(businessId, category).stream()
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

        // List<Long> → String 변환
        String staffIdsString = request.getStaffIds() != null
                ? Service.staffIdsToString(request.getStaffIds())
                : service.getStaffIds();

        Service updatedService = Service.builder()
                .id(service.getId())
                .businessId(service.getBusinessId())
                .category(request.getCategory() != null ? request.getCategory() : service.getCategory())
                .name(request.getName() != null ? request.getName() : service.getName())
                .description(request.getDescription() != null ? request.getDescription() : service.getDescription())
                .duration(request.getDuration() != null ? request.getDuration() : service.getDuration())
                .price(request.getPrice() != null ? request.getPrice() : service.getPrice())
                .staffIds(staffIdsString)
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
}