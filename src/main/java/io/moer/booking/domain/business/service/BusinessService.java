package io.moer.booking.domain.business.service;

import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.BusinessSettings;
import io.moer.booking.domain.business.BusinessStatus;
import io.moer.booking.domain.business.dto.BusinessCreateRequest;
import io.moer.booking.domain.business.dto.BusinessResponse;
import io.moer.booking.domain.business.dto.BusinessSearchCondition;
import io.moer.booking.domain.business.dto.BusinessUpdateRequest;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.business.repository.BusinessSettingsRepository;
import io.moer.booking.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 매장 서비스
 */
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
                .phone(request.getPhone())
                .address(request.getAddress())
                .description(request.getDescription())
                .businessHours(request.getBusinessHours())
                .status(BusinessStatus.ACTIVE)
                .build();

        businessRepository.save(business);

        // BusinessSettings 기본값으로 생성
        BusinessSettings settings = BusinessSettings.builder()
                .businessId(business.getId())
                .bookingInterval(30)
                .autoConfirm("N")
                .allowOnlineBooking("Y")
                .maxAdvanceBookingDays(30)
                .minAdvanceBookingHours(2)
                .sendConfirmationSms("Y")
                .sendReminderSms("Y")
                .reminderHoursBefore(24)
                .sendCancelSms("Y")
                .kakaoEnabled("N")
                .paymentMethods("CARD,CASH")
                .requireDeposit("N")
                .depositAmount(0)
                .allowCancellation("Y")
                .cancelDeadlineHours(24)
                .noShowPenaltyEnabled("N")
                .timezone("Asia/Seoul")
                .language("ko")
                .build();

        businessSettingsRepository.save(settings);

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
        List<BusinessResponse> content = businessRepository.findAll(condition).stream()
                .map(business -> {
                    BusinessSettings settings = businessSettingsRepository
                            .findByBusinessId(business.getId())
                            .orElse(null);
                    return BusinessResponse.from(business, settings);
                })
                .collect(Collectors.toList());

        long totalElements = businessRepository.countAll(condition);

        return PageResponse.of(content, condition.getPage(), condition.getSize(), totalElements);
    }

    /**
     * Owner의 매장 목록 조회
     */
    public List<BusinessResponse> getBusinessesByOwner(Long ownerId) {
        userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        return businessRepository.findByOwnerId(ownerId).stream()
                .map(business -> {
                    BusinessSettings settings = businessSettingsRepository
                            .findByBusinessId(business.getId())
                            .orElse(null);
                    return BusinessResponse.from(business, settings);
                })
                .collect(Collectors.toList());
    }

    /**
     * 매장 수정
     */
    @Transactional
    public BusinessResponse updateBusiness(Long id, BusinessUpdateRequest request) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        Business updatedBusiness = Business.builder()
                .id(business.getId())
                .ownerId(business.getOwnerId())
                .businessType(business.getBusinessType())
                .name(request.getName() != null ? request.getName() : business.getName())
                .phone(request.getPhone() != null ? request.getPhone() : business.getPhone())
                .address(request.getAddress() != null ? request.getAddress() : business.getAddress())
                .description(request.getDescription() != null ? request.getDescription() : business.getDescription())
                .businessHours(request.getBusinessHours() != null ? request.getBusinessHours() : business.getBusinessHours())
                .status(business.getStatus())
                .createdAt(business.getCreatedAt())
                .build();

        businessRepository.update(updatedBusiness);

        log.info("Business updated: id={}", id);

        return getBusinessWithSettings(id);
    }

    /**
     * 매장 Settings 수정
     */
    @Transactional
    public BusinessResponse updateBusinessSettings(Long id, BusinessSettings newSettings) {
        if (!businessRepository.existsById(id)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        BusinessSettings existing = businessSettingsRepository.findByBusinessId(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND,
                        "Settings를 찾을 수 없습니다"));

        // 업데이트할 필드만 변경
        BusinessSettings updated = BusinessSettings.builder()
                .id(existing.getId())
                .businessId(id)
                .bookingInterval(newSettings.getBookingInterval() != null ? newSettings.getBookingInterval() : existing.getBookingInterval())
                .autoConfirm(newSettings.getAutoConfirm() != null ? newSettings.getAutoConfirm() : existing.getAutoConfirm())
                .allowOnlineBooking(newSettings.getAllowOnlineBooking() != null ? newSettings.getAllowOnlineBooking() : existing.getAllowOnlineBooking())
                .maxAdvanceBookingDays(newSettings.getMaxAdvanceBookingDays() != null ? newSettings.getMaxAdvanceBookingDays() : existing.getMaxAdvanceBookingDays())
                .minAdvanceBookingHours(newSettings.getMinAdvanceBookingHours() != null ? newSettings.getMinAdvanceBookingHours() : existing.getMinAdvanceBookingHours())
                .sendConfirmationSms(newSettings.getSendConfirmationSms() != null ? newSettings.getSendConfirmationSms() : existing.getSendConfirmationSms())
                .sendReminderSms(newSettings.getSendReminderSms() != null ? newSettings.getSendReminderSms() : existing.getSendReminderSms())
                .reminderHoursBefore(newSettings.getReminderHoursBefore() != null ? newSettings.getReminderHoursBefore() : existing.getReminderHoursBefore())
                .sendCancelSms(newSettings.getSendCancelSms() != null ? newSettings.getSendCancelSms() : existing.getSendCancelSms())
                .kakaoChannelId(newSettings.getKakaoChannelId() != null ? newSettings.getKakaoChannelId() : existing.getKakaoChannelId())
                .kakaoApiKey(newSettings.getKakaoApiKey() != null ? newSettings.getKakaoApiKey() : existing.getKakaoApiKey())
                .kakaoEnabled(newSettings.getKakaoEnabled() != null ? newSettings.getKakaoEnabled() : existing.getKakaoEnabled())
                .paymentMethods(newSettings.getPaymentMethods() != null ? newSettings.getPaymentMethods() : existing.getPaymentMethods())
                .requireDeposit(newSettings.getRequireDeposit() != null ? newSettings.getRequireDeposit() : existing.getRequireDeposit())
                .depositAmount(newSettings.getDepositAmount() != null ? newSettings.getDepositAmount() : existing.getDepositAmount())
                .allowCancellation(newSettings.getAllowCancellation() != null ? newSettings.getAllowCancellation() : existing.getAllowCancellation())
                .cancelDeadlineHours(newSettings.getCancelDeadlineHours() != null ? newSettings.getCancelDeadlineHours() : existing.getCancelDeadlineHours())
                .noShowPenaltyEnabled(newSettings.getNoShowPenaltyEnabled() != null ? newSettings.getNoShowPenaltyEnabled() : existing.getNoShowPenaltyEnabled())
                .timezone(newSettings.getTimezone() != null ? newSettings.getTimezone() : existing.getTimezone())
                .language(newSettings.getLanguage() != null ? newSettings.getLanguage() : existing.getLanguage())
                .createdAt(existing.getCreatedAt())
                .build();

        businessSettingsRepository.update(updated);

        log.info("Business settings updated: businessId={}", id);

        return getBusinessWithSettings(id);
    }

    /**
     * 매장 삭제
     */
    @Transactional
    public void deleteBusiness(Long id) {
        if (!businessRepository.existsById(id)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        businessSettingsRepository.deleteByBusinessId(id);
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
                .ownerId(business.getOwnerId())
                .businessType(business.getBusinessType())
                .name(business.getName())
                .phone(business.getPhone())
                .address(business.getAddress())
                .description(business.getDescription())
                .businessHours(business.getBusinessHours())
                .status(status)
                .createdAt(business.getCreatedAt())
                .build();

        businessRepository.update(updated);

        log.info("Business status changed: id={}, status={}", id, status);

        return getBusinessWithSettings(id);
    }

    // === Private Methods ===

    private BusinessResponse getBusinessWithSettings(Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        BusinessSettings settings = businessSettingsRepository
                .findByBusinessId(businessId)
                .orElse(null);

        return BusinessResponse.from(business, settings);
    }
}