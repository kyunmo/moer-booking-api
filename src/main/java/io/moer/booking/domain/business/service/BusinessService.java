package io.moer.booking.domain.business.service;

import io.moer.booking.common.dto.PageInfo;
import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.BusinessSettings;
import io.moer.booking.domain.business.BusinessStatus;
import io.moer.booking.domain.business.dto.*;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.business.repository.BusinessSettingsRepository;
import io.moer.booking.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessSettingsRepository businessSettingsRepository;
    private final UserRepository userRepository;

    /**
     * 매장 생성
     */
    @Transactional
    public BusinessResponse createBusiness(BusinessCreateRequest request) {
        // Owner 존재 확인
        userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        // Business 엔티티 생성
        Business business = Business.builder()
                .ownerId(request.getOwnerId())
                .businessType(request.getBusinessType())
                .name(request.getName())
                .description(request.getDescription())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .addressDetail(request.getAddressDetail())
                .zipCode(request.getZipCode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .openingHours(request.getOpeningHours())
                .regularHolidays(request.getRegularHolidays())
                .logoUrl(request.getLogoUrl())
                .coverImageUrl(request.getCoverImageUrl())
                .images(request.getImages())
                .website(request.getWebsite())
                .instagram(request.getInstagram())
                .facebook(request.getFacebook())
                .status(BusinessStatus.ACTIVE)
                .build();

        // 저장
        businessRepository.save(business);

        // Settings 저장 (있으면)
        if (request.getSettings() != null && !request.getSettings().isEmpty()) {
            BusinessSettings settings = BusinessSettings.builder()
                    .businessId(business.getId())
                    .settings(request.getSettings())
                    .build();
            businessSettingsRepository.save(settings);
        }

        log.info("Business created: id={}, name={}, ownerId={}",
                business.getId(), business.getName(), business.getOwnerId());

        return getBusinessWithSettings(business.getId());
    }

    /**
     * 매장 단건 조회 (Settings 포함)
     */
    public BusinessResponse getBusiness(Long id) {
        return getBusinessWithSettings(id);
    }

    /**
     * 매장 목록 조회 (페이징)
     */
    public PageResponse<BusinessResponse> getBusinesses(BusinessSearchCondition condition) {
        // 데이터 조회
        List<BusinessResponse> content = businessRepository.findAll(condition).stream()
                .map(business -> {
                    // Settings 조회
                    Map<String, Object> settings = businessSettingsRepository
                            .findByBusinessId(business.getId())
                            .map(BusinessSettings::getSettings)
                            .orElse(new HashMap<>());

                    return BusinessResponse.from(business, settings);
                })
                .collect(Collectors.toList());

        // 전체 개수
        long totalElements = businessRepository.countAll(condition);

        // 페이징 정보
        PageInfo pageInfo = new PageInfo(
                condition.getPage(),
                condition.getSize(),
                totalElements
        );

        //return PageResponse.of(content, pageInfo);
        return PageResponse.of(content, condition.getPage(), condition.getSize(), totalElements);
    }

    /**
     * Owner의 매장 목록 조회
     */
    public List<BusinessResponse> getBusinessesByOwner(Long ownerId) {
        // Owner 존재 확인
        userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        return businessRepository.findByOwnerId(ownerId).stream()
                .map(business -> {
                    Map<String, Object> settings = businessSettingsRepository
                            .findByBusinessId(business.getId())
                            .map(BusinessSettings::getSettings)
                            .orElse(new HashMap<>());

                    return BusinessResponse.from(business, settings);
                })
                .collect(Collectors.toList());
    }

    /**
     * 매장 수정
     */
    @Transactional
    public BusinessResponse updateBusiness(Long id, BusinessUpdateRequest request) {
        // 존재 확인
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        // 수정할 필드만 업데이트
        Business updatedBusiness = Business.builder()
                .id(business.getId())
                .name(request.getName() != null ? request.getName() : business.getName())
                .description(request.getDescription() != null ? request.getDescription() : business.getDescription())
                .phone(request.getPhone() != null ? request.getPhone() : business.getPhone())
                .email(request.getEmail() != null ? request.getEmail() : business.getEmail())
                .address(request.getAddress() != null ? request.getAddress() : business.getAddress())
                .addressDetail(request.getAddressDetail())
                .zipCode(request.getZipCode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .openingHours(request.getOpeningHours())
                .regularHolidays(request.getRegularHolidays())
                .logoUrl(request.getLogoUrl())
                .coverImageUrl(request.getCoverImageUrl())
                .images(request.getImages())
                .website(request.getWebsite())
                .instagram(request.getInstagram())
                .facebook(request.getFacebook())
                .build();

        businessRepository.update(updatedBusiness);

        log.info("Business updated: id={}", id);

        return getBusinessWithSettings(id);
    }

    /**
     * 매장 Settings 수정
     */
    @Transactional
    public BusinessResponse updateBusinessSettings(Long id, Map<String, Object> newSettings) {
        // Business 존재 확인
        if (!businessRepository.existsById(id)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        // Settings 조회 또는 생성
        BusinessSettings settings = businessSettingsRepository.findByBusinessId(id)
                .orElse(BusinessSettings.builder()
                        .businessId(id)
                        .settings(new HashMap<>())
                        .build());

        // 기존 settings 업데이트
        Map<String, Object> updatedSettings = new HashMap<>(settings.getSettings());
        updatedSettings.putAll(newSettings);

        BusinessSettings updated = BusinessSettings.builder()
                .businessId(id)
                .settings(updatedSettings)
                .build();

        if (settings.getId() == null) {
            businessSettingsRepository.save(updated);
        } else {
            businessSettingsRepository.update(updated);
        }

        log.info("Business settings updated: businessId={}", id);

        return getBusinessWithSettings(id);
    }

    /**
     * 매장 삭제
     */
    @Transactional
    public void deleteBusiness(Long id) {
        // 존재 확인
        if (!businessRepository.existsById(id)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        // Settings 먼저 삭제 (FK 제약)
        businessSettingsRepository.deleteByBusinessId(id);

        // Business 삭제
        businessRepository.delete(id);

        log.info("Business deleted: id={}", id);
    }

    /**
     * 매장 상태 변경
     */
    @Transactional
    public BusinessResponse changeBusinessStatus(Long id, BusinessStatus status) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        Business updated = Business.builder()
                .id(business.getId())
                .status(status)
                .build();

        businessRepository.update(updated);

        log.info("Business status changed: id={}, status={}", id, status);

        return getBusinessWithSettings(id);
    }

    // === Private Methods ===

    private BusinessResponse getBusinessWithSettings(Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        Map<String, Object> settings = businessSettingsRepository
                .findByBusinessId(businessId)
                .map(BusinessSettings::getSettings)
                .orElse(new HashMap<>());

        return BusinessResponse.from(business, settings);
    }
}