package io.moer.booking.domain.staff.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.common.storage.FileStorageService;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.staff.Staff;
import io.moer.booking.domain.staff.dto.StaffCreateRequest;
import io.moer.booking.domain.staff.dto.StaffResponse;
import io.moer.booking.domain.staff.dto.StaffSearchCondition;
import io.moer.booking.domain.staff.dto.StaffUpdateRequest;
import io.moer.booking.domain.staff.position.StaffPosition;
import io.moer.booking.domain.staff.position.repository.StaffPositionRepository;
import io.moer.booking.domain.staff.repository.StaffRepository;
import io.moer.booking.domain.subscription.service.UsageLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final FileStorageService fileStorageService;
    private final StaffPositionRepository staffPositionRepository;
    private final io.moer.booking.domain.business.service.OnboardingService onboardingService;

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

        // positionId 검증 및 position 텍스트 자동 채움
        String positionText = request.getPosition();
        if (request.getPositionId() != null) {
            StaffPosition position = staffPositionRepository.findById(request.getPositionId())
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STAFF_POSITION_NOT_FOUND));
            if (!position.getBusinessId().equals(businessId)) {
                throw new EntityNotFoundException(ErrorCode.STAFF_POSITION_NOT_FOUND);
            }
            positionText = position.getName();
        }

        Staff staff = Staff.builder()
                .businessId(businessId)
                .name(request.getName())
                .position(positionText)
                .positionId(request.getPositionId())
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

        // 온보딩 스텝 자동 완료
        onboardingService.markStepComplete(businessId, "staff");

        return toResponse(staff);
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

        return toResponse(staff);
    }

    /**
     * Business의 전체 직원 조회
     */
    public List<StaffResponse> getStaffsByBusiness(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return staffRepository.findByBusinessId(businessId).stream()
                .map(this::toResponse)
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
                .map(this::toResponse)
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

        // positionId 검증 및 position 텍스트 자동 채움
        Long newPositionId = request.getPositionId() != null ? request.getPositionId() : staff.getPositionId();
        String newPosition = request.getPosition() != null ? request.getPosition() : staff.getPosition();

        if (request.getPositionId() != null) {
            StaffPosition position = staffPositionRepository.findById(request.getPositionId())
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STAFF_POSITION_NOT_FOUND));
            if (!position.getBusinessId().equals(businessId)) {
                throw new EntityNotFoundException(ErrorCode.STAFF_POSITION_NOT_FOUND);
            }
            newPosition = position.getName();
        }

        // Builder로 모든 필드 명시 (null이면 기존 값 유지)
        Staff updatedStaff = Staff.builder()
                .id(staff.getId())
                .businessId(staff.getBusinessId())
                .name(request.getName() != null ? request.getName() : staff.getName())
                .position(newPosition)
                .positionId(newPositionId)
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
     * 직원 검색
     */
    public List<StaffResponse> searchStaffs(Long businessId, StaffSearchCondition condition) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        StaffSearchCondition searchCondition = StaffSearchCondition.builder()
                .businessId(businessId)
                .name(condition.getName())
                .positionId(condition.getPositionId())
                .specialty(condition.getSpecialty())
                .isActive(condition.getIsActive())
                .minCareerYears(condition.getMinCareerYears())
                .sortBy(condition.getSortBy())
                .sortOrder(condition.getSortOrder())
                .build();

        return staffRepository.findByCondition(searchCondition).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
     * 직원 프로필 이미지 업로드
     */
    @Transactional
    public StaffResponse uploadProfileImage(Long businessId, Long staffId, MultipartFile file) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STAFF_NOT_FOUND));

        if (!staff.getBusinessId().equals(businessId)) {
            throw new BusinessException(ErrorCode.STAFF_ACCESS_DENIED,
                    "해당 직원에 접근 권한이 없습니다");
        }

        // 기존 이미지 삭제
        if (staff.getProfileImageUrl() != null) {
            fileStorageService.delete(staff.getProfileImageUrl());
        }

        // 새 이미지 저장
        String imageUrl = fileStorageService.store(file, "staff-profiles");
        staffRepository.updateProfileImageUrl(staffId, imageUrl);

        log.info("Staff profile image uploaded: staffId={}, imageUrl={}", staffId, imageUrl);

        Staff updatedStaff = staffRepository.findById(staffId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STAFF_NOT_FOUND));
        return toResponse(updatedStaff);
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

    /**
     * Staff → StaffResponse 변환 (positionName 자동 매핑)
     */
    private StaffResponse toResponse(Staff staff) {
        if (staff.getPositionId() != null) {
            String positionName = staffPositionRepository.findById(staff.getPositionId())
                    .map(StaffPosition::getName)
                    .orElse(null);
            return StaffResponse.from(staff, positionName);
        }
        return StaffResponse.from(staff);
    }
}
