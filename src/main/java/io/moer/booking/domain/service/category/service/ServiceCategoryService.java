package io.moer.booking.domain.service.category.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.service.category.ServiceCategory;
import io.moer.booking.domain.service.category.dto.ServiceCategoryCreateRequest;
import io.moer.booking.domain.service.category.dto.ServiceCategoryResponse;
import io.moer.booking.domain.service.category.dto.ServiceCategoryUpdateRequest;
import io.moer.booking.domain.service.category.dto.SortOrderUpdateRequest;
import io.moer.booking.domain.service.category.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 서비스 카테고리 관리 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceCategoryService {

    private final ServiceCategoryRepository serviceCategoryRepository;
    private final BusinessRepository businessRepository;

    /**
     * 카테고리 생성
     */
    @Transactional
    public ServiceCategoryResponse createCategory(Long businessId, ServiceCategoryCreateRequest request) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        // 중복명 체크
        if (serviceCategoryRepository.existsByBusinessIdAndName(businessId, request.getName())) {
            throw new BusinessException(ErrorCode.SERVICE_CATEGORY_DUPLICATE_NAME);
        }

        // sortOrder 자동 설정 (max + 1)
        int maxSortOrder = serviceCategoryRepository.getMaxSortOrderByBusinessId(businessId);

        ServiceCategory category = ServiceCategory.builder()
                .businessId(businessId)
                .name(request.getName())
                .description(request.getDescription())
                .sortOrder(maxSortOrder + 1)
                .build();

        serviceCategoryRepository.save(category);

        log.info("ServiceCategory created: id={}, businessId={}, name={}",
                category.getId(), businessId, category.getName());

        return ServiceCategoryResponse.from(category);
    }

    /**
     * 매장의 카테고리 목록 조회 (sort_order 순, serviceCount 포함)
     */
    public List<ServiceCategoryResponse> getCategoriesByBusiness(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return serviceCategoryRepository.findByBusinessId(businessId).stream()
                .map(category -> {
                    int serviceCount = serviceCategoryRepository.countServicesByCategoryId(category.getId());
                    return ServiceCategoryResponse.from(category, serviceCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 카테고리 단건 조회
     */
    public ServiceCategoryResponse getCategory(Long businessId, Long categoryId) {
        if (!serviceCategoryRepository.existsByBusinessIdAndId(businessId, categoryId)) {
            throw new EntityNotFoundException(ErrorCode.SERVICE_CATEGORY_NOT_FOUND);
        }

        ServiceCategory category = serviceCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SERVICE_CATEGORY_NOT_FOUND));

        int serviceCount = serviceCategoryRepository.countServicesByCategoryId(categoryId);
        return ServiceCategoryResponse.from(category, serviceCount);
    }

    /**
     * 카테고리 수정
     */
    @Transactional
    public ServiceCategoryResponse updateCategory(Long businessId, Long categoryId, ServiceCategoryUpdateRequest request) {
        if (!serviceCategoryRepository.existsByBusinessIdAndId(businessId, categoryId)) {
            throw new EntityNotFoundException(ErrorCode.SERVICE_CATEGORY_NOT_FOUND);
        }

        ServiceCategory category = serviceCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SERVICE_CATEGORY_NOT_FOUND));

        // 이름 변경 시 중복 체크 (자기 자신 제외)
        String newName = request.getName() != null ? request.getName() : category.getName();
        if (request.getName() != null &&
                serviceCategoryRepository.existsByBusinessIdAndNameAndIdNot(businessId, newName, categoryId)) {
            throw new BusinessException(ErrorCode.SERVICE_CATEGORY_DUPLICATE_NAME);
        }

        ServiceCategory updatedCategory = ServiceCategory.builder()
                .id(category.getId())
                .businessId(category.getBusinessId())
                .name(newName)
                .description(request.getDescription() != null ? request.getDescription() : category.getDescription())
                .sortOrder(category.getSortOrder())
                .createdAt(category.getCreatedAt())
                .build();

        serviceCategoryRepository.update(updatedCategory);

        log.info("ServiceCategory updated: id={}, businessId={}", categoryId, businessId);

        return getCategory(businessId, categoryId);
    }

    /**
     * 카테고리 삭제 (서비스 존재 시 차단)
     */
    @Transactional
    public void deleteCategory(Long businessId, Long categoryId) {
        if (!serviceCategoryRepository.existsByBusinessIdAndId(businessId, categoryId)) {
            throw new EntityNotFoundException(ErrorCode.SERVICE_CATEGORY_NOT_FOUND);
        }

        // 서비스 존재 시 삭제 차단
        int serviceCount = serviceCategoryRepository.countServicesByCategoryId(categoryId);
        if (serviceCount > 0) {
            throw new BusinessException(ErrorCode.SERVICE_CATEGORY_HAS_SERVICES);
        }

        serviceCategoryRepository.delete(categoryId);

        log.info("ServiceCategory deleted: id={}, businessId={}", categoryId, businessId);
    }

    /**
     * 카테고리 정렬 순서 일괄 변경
     */
    @Transactional
    public List<ServiceCategoryResponse> updateSortOrder(Long businessId, SortOrderUpdateRequest request) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        for (SortOrderUpdateRequest.SortOrderItem item : request.getItems()) {
            if (!serviceCategoryRepository.existsByBusinessIdAndId(businessId, item.getId())) {
                throw new BusinessException(ErrorCode.SERVICE_CATEGORY_SORT_ORDER_INVALID,
                        "존재하지 않는 카테고리 ID: " + item.getId());
            }
            serviceCategoryRepository.updateSortOrder(item.getId(), item.getSortOrder());
        }

        log.info("ServiceCategory sort order updated: businessId={}, count={}",
                businessId, request.getItems().size());

        return getCategoriesByBusiness(businessId);
    }
}
