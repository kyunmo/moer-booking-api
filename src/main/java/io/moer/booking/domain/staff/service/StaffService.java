package io.moer.booking.domain.staff.service;

import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.staff.Staff;
import io.moer.booking.domain.staff.dto.StaffCreateRequest;
import io.moer.booking.domain.staff.dto.StaffResponse;
import io.moer.booking.domain.staff.dto.StaffUpdateRequest;
import io.moer.booking.domain.staff.repository.StaffRepository;
import io.moer.booking.domain.subscription.service.UsageLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 직원 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffService {

    private final StaffRepository staffRepository;
    private final BusinessRepository businessRepository;
    private final UsageLimitService usageLimitService;

    /**
     * 직원 생성
     */
    @Transactional
    public StaffResponse createStaff(Long businessId, StaffCreateRequest request) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        // 직원 수 제한 체크
        usageLimitService.checkCanAddStaff(businessId);

        Staff staff = Staff.builder()
                .businessId(businessId)
                .name(request.getName())
                .position(request.getPosition())
                .phone(request.getPhone())
                .email(request.getEmail())
                .specialty(request.getSpecialty())
                .careerYears(request.getCareerYears())
                .profileImageUrl(request.getProfileImageUrl())
                .introduction(request.getIntroduction())
                .isActive("Y")
                .build();

        staffRepository.save(staff);

        // 직원 수 증가
        usageLimitService.incrementStaffCount(businessId);

        log.info("Staff created: id={}, businessId={}, name={}",
                staff.getId(), businessId, staff.getName());

        return StaffResponse.from(staff);
    }

    /**
     * 직원 단건 조회
     */
    public StaffResponse getStaff(Long businessId, Long staffId) {
        if (!staffRepository.existsByBusinessIdAndId(businessId, staffId)) {
            throw new EntityNotFoundException(ErrorCode.STAFF_NOT_FOUND);
        }

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STAFF_NOT_FOUND));

        return StaffResponse.from(staff);
    }

    /**
     * Business의 전체 직원 조회
     */
    public List<StaffResponse> getStaffsByBusiness(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return staffRepository.findByBusinessId(businessId).stream()
                .map(StaffResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Business의 활성 직원 조회
     */
    public List<StaffResponse> getActiveStaffsByBusiness(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return staffRepository.findActiveByBusinessId(businessId).stream()
                .map(StaffResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 직원 수정
     */
    @Transactional
    public StaffResponse updateStaff(Long businessId, Long staffId, StaffUpdateRequest request) {
        if (!staffRepository.existsByBusinessIdAndId(businessId, staffId)) {
            throw new EntityNotFoundException(ErrorCode.STAFF_NOT_FOUND);
        }

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STAFF_NOT_FOUND));

        // Builder로 모든 필드 명시 (null이면 기존 값 유지)
        Staff updatedStaff = Staff.builder()
                .id(staff.getId())
                .businessId(staff.getBusinessId())
                .name(request.getName() != null ? request.getName() : staff.getName())
                .position(request.getPosition() != null ? request.getPosition() : staff.getPosition())
                .phone(request.getPhone() != null ? request.getPhone() : staff.getPhone())
                .email(request.getEmail() != null ? request.getEmail() : staff.getEmail())
                .specialty(request.getSpecialty() != null ? request.getSpecialty() : staff.getSpecialty())
                .careerYears(request.getCareerYears() != null ? request.getCareerYears() : staff.getCareerYears())
                .profileImageUrl(request.getProfileImageUrl() != null ?
                        request.getProfileImageUrl() : staff.getProfileImageUrl())
                .introduction(request.getIntroduction() != null ?
                        request.getIntroduction() : staff.getIntroduction())
                .isActive(staff.getIsActive())
                .createdAt(staff.getCreatedAt())
                .build();

        staffRepository.update(updatedStaff);

        log.info("Staff updated: id={}, businessId={}", staffId, businessId);

        return getStaff(businessId, staffId);
    }

    /**
     * 직원 활성/비활성 전환
     */
    @Transactional
    public StaffResponse toggleStaffActive(Long businessId, Long staffId) {
        if (!staffRepository.existsByBusinessIdAndId(businessId, staffId)) {
            throw new EntityNotFoundException(ErrorCode.STAFF_NOT_FOUND);
        }

        staffRepository.toggleActive(staffId);

        log.info("Staff active toggled: id={}, businessId={}", staffId, businessId);

        return getStaff(businessId, staffId);
    }

    /**
     * 직원 삭제
     */
    @Transactional
    public void deleteStaff(Long businessId, Long staffId) {
        if (!staffRepository.existsByBusinessIdAndId(businessId, staffId)) {
            throw new EntityNotFoundException(ErrorCode.STAFF_NOT_FOUND);
        }

        staffRepository.delete(staffId);

        // 직원 수 감소
        usageLimitService.decrementStaffCount(businessId);

        log.info("Staff deleted: id={}, businessId={}", staffId, businessId);
    }
}