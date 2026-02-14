package io.moer.booking.domain.staff.position.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.service.category.dto.SortOrderUpdateRequest;
import io.moer.booking.domain.staff.position.StaffPosition;
import io.moer.booking.domain.staff.position.dto.StaffPositionCreateRequest;
import io.moer.booking.domain.staff.position.dto.StaffPositionResponse;
import io.moer.booking.domain.staff.position.dto.StaffPositionUpdateRequest;
import io.moer.booking.domain.staff.position.repository.StaffPositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 직급 관리 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffPositionService {

    private final StaffPositionRepository staffPositionRepository;
    private final BusinessRepository businessRepository;

    /**
     * 직급 생성
     */
    @Transactional
    public StaffPositionResponse createPosition(Long businessId, StaffPositionCreateRequest request) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        // 중복명 체크
        if (staffPositionRepository.existsByBusinessIdAndName(businessId, request.getName())) {
            throw new BusinessException(ErrorCode.STAFF_POSITION_DUPLICATE_NAME);
        }

        // sortOrder 자동 설정 (max + 1)
        int maxSortOrder = staffPositionRepository.getMaxSortOrderByBusinessId(businessId);

        StaffPosition position = StaffPosition.builder()
                .businessId(businessId)
                .name(request.getName())
                .description(request.getDescription())
                .sortOrder(maxSortOrder + 1)
                .build();

        staffPositionRepository.save(position);

        log.info("StaffPosition created: id={}, businessId={}, name={}",
                position.getId(), businessId, position.getName());

        return StaffPositionResponse.from(position);
    }

    /**
     * 매장의 직급 목록 조회 (sort_order 순, staffCount 포함)
     */
    public List<StaffPositionResponse> getPositionsByBusiness(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return staffPositionRepository.findByBusinessId(businessId).stream()
                .map(position -> {
                    int staffCount = staffPositionRepository.countStaffsByPositionId(position.getId());
                    return StaffPositionResponse.from(position, staffCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 직급 단건 조회
     */
    public StaffPositionResponse getPosition(Long businessId, Long positionId) {
        if (!staffPositionRepository.existsByBusinessIdAndId(businessId, positionId)) {
            throw new EntityNotFoundException(ErrorCode.STAFF_POSITION_NOT_FOUND);
        }

        StaffPosition position = staffPositionRepository.findById(positionId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STAFF_POSITION_NOT_FOUND));

        int staffCount = staffPositionRepository.countStaffsByPositionId(positionId);
        return StaffPositionResponse.from(position, staffCount);
    }

    /**
     * 직급 수정
     */
    @Transactional
    public StaffPositionResponse updatePosition(Long businessId, Long positionId, StaffPositionUpdateRequest request) {
        if (!staffPositionRepository.existsByBusinessIdAndId(businessId, positionId)) {
            throw new EntityNotFoundException(ErrorCode.STAFF_POSITION_NOT_FOUND);
        }

        StaffPosition position = staffPositionRepository.findById(positionId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STAFF_POSITION_NOT_FOUND));

        // 이름 변경 시 중복 체크 (자기 자신 제외)
        String newName = request.getName() != null ? request.getName() : position.getName();
        if (request.getName() != null &&
                staffPositionRepository.existsByBusinessIdAndNameAndIdNot(businessId, newName, positionId)) {
            throw new BusinessException(ErrorCode.STAFF_POSITION_DUPLICATE_NAME);
        }

        StaffPosition updatedPosition = StaffPosition.builder()
                .id(position.getId())
                .businessId(position.getBusinessId())
                .name(newName)
                .description(request.getDescription() != null ? request.getDescription() : position.getDescription())
                .sortOrder(position.getSortOrder())
                .createdAt(position.getCreatedAt())
                .build();

        staffPositionRepository.update(updatedPosition);

        log.info("StaffPosition updated: id={}, businessId={}", positionId, businessId);

        return getPosition(businessId, positionId);
    }

    /**
     * 직급 삭제 (직원 존재 시 차단)
     */
    @Transactional
    public void deletePosition(Long businessId, Long positionId) {
        if (!staffPositionRepository.existsByBusinessIdAndId(businessId, positionId)) {
            throw new EntityNotFoundException(ErrorCode.STAFF_POSITION_NOT_FOUND);
        }

        // 직원 존재 시 삭제 차단
        int staffCount = staffPositionRepository.countStaffsByPositionId(positionId);
        if (staffCount > 0) {
            throw new BusinessException(ErrorCode.STAFF_POSITION_HAS_STAFFS);
        }

        staffPositionRepository.delete(positionId);

        log.info("StaffPosition deleted: id={}, businessId={}", positionId, businessId);
    }

    /**
     * 직급 정렬 순서 일괄 변경
     */
    @Transactional
    public List<StaffPositionResponse> updateSortOrder(Long businessId, SortOrderUpdateRequest request) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        for (SortOrderUpdateRequest.SortOrderItem item : request.getItems()) {
            if (!staffPositionRepository.existsByBusinessIdAndId(businessId, item.getId())) {
                throw new BusinessException(ErrorCode.STAFF_POSITION_SORT_ORDER_INVALID,
                        "존재하지 않는 직급 ID: " + item.getId());
            }
            staffPositionRepository.updateSortOrder(item.getId(), item.getSortOrder());
        }

        log.info("StaffPosition sort order updated: businessId={}, count={}",
                businessId, request.getItems().size());

        return getPositionsByBusiness(businessId);
    }
}
