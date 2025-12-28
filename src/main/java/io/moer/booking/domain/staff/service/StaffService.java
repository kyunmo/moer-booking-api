package io.moer.booking.domain.staff.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.staff.Staff;
import io.moer.booking.domain.staff.dto.StaffCreateRequest;
import io.moer.booking.domain.staff.dto.StaffResponse;
import io.moer.booking.domain.staff.dto.StaffUpdateRequest;
import io.moer.booking.domain.staff.repository.StaffRepository;
import io.moer.booking.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffService {

    private final StaffRepository staffRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;

    /**
     * Staff 생성
     */
    @Transactional
    public StaffResponse createStaff(Long businessId, StaffCreateRequest request) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        // User 존재 확인 (userId가 있는 경우)
        if (request.getUserId() != null) {
            if (!userRepository.findById(request.getUserId()).isPresent()) {
                throw new EntityNotFoundException(ErrorCode.USER_NOT_FOUND);
            }
        }

        // Staff 엔티티 생성
        Staff staff = Staff.builder()
                .businessId(businessId)
                .userId(request.getUserId())
                .name(request.getName())
                .nickname(request.getNickname())
                .phone(request.getPhone())
                .email(request.getEmail())
                .profileImageUrl(request.getProfileImageUrl())
                .introduction(request.getIntroduction())
                .careerYears(request.getCareerYears())
                .specialties(request.getSpecialties())
                .workSchedule(request.getWorkSchedule())
                .isActive(true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();

        // 저장
        staffRepository.save(staff);

        log.info("Staff created: id={}, businessId={}, name={}",
                staff.getId(), businessId, staff.getName());

        return StaffResponse.from(staff);
    }

    /**
     * Staff 단건 조회
     */
    public StaffResponse getStaff(Long businessId, Long staffId) {
        // Business의 Staff인지 확인
        if (!staffRepository.existsByBusinessIdAndId(businessId, staffId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        return StaffResponse.from(staff);
    }

    /**
     * Business의 Staff 목록 조회
     */
    public List<StaffResponse> getStaffsByBusiness(Long businessId) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return staffRepository.findByBusinessId(businessId).stream()
                .map(StaffResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Business의 활성 Staff 목록 조회
     */
    public List<StaffResponse> getActiveStaffsByBusiness(Long businessId) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return staffRepository.findByBusinessIdAndActive(businessId, true).stream()
                .map(StaffResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Staff 수정
     */
    @Transactional
    public StaffResponse updateStaff(Long businessId, Long staffId, StaffUpdateRequest request) {
        // Business의 Staff인지 확인
        if (!staffRepository.existsByBusinessIdAndId(businessId, staffId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        // 수정할 필드만 업데이트
        Staff updatedStaff = Staff.builder()
                .id(staff.getId())
                .name(request.getName() != null ? request.getName() : staff.getName())
                .nickname(request.getNickname() != null ? request.getNickname() : staff.getNickname())
                .phone(request.getPhone() != null ? request.getPhone() : staff.getPhone())
                .email(request.getEmail() != null ? request.getEmail() : staff.getEmail())
                .profileImageUrl(request.getProfileImageUrl() != null ?
                        request.getProfileImageUrl() : staff.getProfileImageUrl())
                .introduction(request.getIntroduction() != null ?
                        request.getIntroduction() : staff.getIntroduction())
                .careerYears(request.getCareerYears() != null ?
                        request.getCareerYears() : staff.getCareerYears())
                .specialties(request.getSpecialties() != null ?
                        request.getSpecialties() : staff.getSpecialties())
                .workSchedule(request.getWorkSchedule() != null ?
                        request.getWorkSchedule() : staff.getWorkSchedule())
                .displayOrder(request.getDisplayOrder() != null ?
                        request.getDisplayOrder() : staff.getDisplayOrder())
                .build();

        staffRepository.update(updatedStaff);

        log.info("Staff updated: id={}, businessId={}", staffId, businessId);

        return getStaff(businessId, staffId);
    }

    /**
     * Staff 활성/비활성 전환
     */
    @Transactional
    public StaffResponse toggleStaffActive(Long businessId, Long staffId) {
        // Business의 Staff인지 확인
        if (!staffRepository.existsByBusinessIdAndId(businessId, staffId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        Staff updated = Staff.builder()
                .id(staff.getId())
                .isActive(!staff.getIsActive())
                .build();

        staffRepository.update(updated);

        log.info("Staff active toggled: id={}, isActive={}", staffId, !staff.getIsActive());

        return getStaff(businessId, staffId);
    }

    /**
     * Staff 삭제
     */
    @Transactional
    public void deleteStaff(Long businessId, Long staffId) {
        // Business의 Staff인지 확인
        if (!staffRepository.existsByBusinessIdAndId(businessId, staffId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        // Portfolio도 자동으로 삭제됨 (CASCADE)
        staffRepository.delete(staffId);

        log.info("Staff deleted: id={}, businessId={}", staffId, businessId);
    }
}