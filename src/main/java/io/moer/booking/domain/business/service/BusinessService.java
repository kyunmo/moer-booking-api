package io.moer.booking.domain.business.service;

import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.common.storage.FileStorageService;
import io.moer.booking.domain.auth.dto.ProfileImageResponse;
import io.moer.booking.domain.auditlog.AuditAction;
import io.moer.booking.domain.auditlog.service.AuditLogService;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.BusinessSettings;
import io.moer.booking.domain.business.BusinessStatus;
import io.moer.booking.domain.business.dto.*;;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.business.repository.BusinessSettingsRepository;
import io.moer.booking.domain.user.User;
import io.moer.booking.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
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
    private final BusinessSettingsService businessSettingsService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final FileStorageService fileStorageService;

    /**
     * 매장 생성
     */
    @Transactional
    public BusinessResponse createBusiness(BusinessCreateRequest request, User currentUser) {
        // Owner 존재 확인
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 임시 slug 자동 생성 (사용자가 나중에 변경 가능)
        String autoSlug = generateUniqueSlug();

        // Business 엔티티 생성
        Business business = Business.builder()
                .ownerId(request.getOwnerId())
                .businessType(request.getBusinessType())
                .name(request.getName())
                .slug(autoSlug)
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

        // 감사 로그 기록
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("businessName", business.getName());
        metadata.put("businessType", business.getBusinessType().name());
        metadata.put("ownerId", business.getOwnerId());
        metadata.put("ownerEmail", owner.getEmail());

        auditLogService.log(
                currentUser,
                AuditAction.BUSINESS_CREATED,
                "Business",
                business.getId(),
                String.format("매장 생성: %s (업종: %s)", business.getName(), business.getBusinessType().getDescription()),
                metadata
        );

        log.info("Business created: id={}, name={}, ownerId={}",
                business.getId(), business.getName(), business.getOwnerId());

        return getBusinessWithSettings(business.getId());
    }

    /**
     * 매장 단건 조회 (Settings 포함)
     */
    public BusinessResponse getBusiness(Long id, User currentUser) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        // 권한 체크
        if (!currentUser.canAccessBusiness(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ACCESS_DENIED);
        }

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
    public BusinessResponse updateBusiness(Long id, BusinessUpdateRequest request, User currentUser) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        // 권한 체크
        if (!currentUser.canAccessBusiness(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ACCESS_DENIED);
        }

        Business updatedBusiness = Business.builder()
                .id(business.getId())
                .ownerId(business.getOwnerId())
                .slug(business.getSlug())
                .businessType(request.getBusinessType() != null ? request.getBusinessType() : business.getBusinessType())
                .name(request.getName() != null ? request.getName() : business.getName())
                .phone(request.getPhone() != null ? request.getPhone() : business.getPhone())
                .address(request.getAddress() != null ? request.getAddress() : business.getAddress())
                .addressDetail(request.getAddressDetail() != null ? request.getAddressDetail() : business.getAddressDetail())
                .zipCode(request.getZipCode() != null ? request.getZipCode() : business.getZipCode())
                .description(request.getDescription() != null ? request.getDescription() : business.getDescription())
                .businessHours(request.getBusinessHours() != null ? request.getBusinessHours() : business.getBusinessHours())
                // 기존 값 보존 (PATCH에서 누락 방지)
                .profileImageUrl(business.getProfileImageUrl())
                .galleryImages(business.getGalleryImages())
                .latitude(request.getLatitude() != null ? request.getLatitude() : business.getLatitude())
                .longitude(request.getLongitude() != null ? request.getLongitude() : business.getLongitude())
                .tags(business.getTags())
                .averageRating(business.getAverageRating())
                .reviewCount(business.getReviewCount())
                .status(business.getStatus())
                .dailyRevenueGoal(request.getDailyRevenueGoal() != null ? request.getDailyRevenueGoal() : business.getDailyRevenueGoal())
                .monthlyRevenueGoal(request.getMonthlyRevenueGoal() != null ? request.getMonthlyRevenueGoal() : business.getMonthlyRevenueGoal())
                .monthlyNewCustomerGoal(request.getMonthlyNewCustomerGoal() != null ? request.getMonthlyNewCustomerGoal() : business.getMonthlyNewCustomerGoal())
                .subscriptionPlan(business.getSubscriptionPlan())
                .billingCycle(business.getBillingCycle())
                .subscriptionStatus(business.getSubscriptionStatus())
                .trialStartedAt(business.getTrialStartedAt())
                .trialEndsAt(business.getTrialEndsAt())
                .subscriptionStartedAt(business.getSubscriptionStartedAt())
                .nextBillingDate(business.getNextBillingDate())
                .currentStaffCount(business.getCurrentStaffCount())
                .currentMonthReservationCount(business.getCurrentMonthReservationCount())
                .createdAt(business.getCreatedAt())
                .build();

        businessRepository.update(updatedBusiness);

        // 감사 로그 기록
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("businessName", updatedBusiness.getName());
        if (!business.getName().equals(updatedBusiness.getName())) {
            metadata.put("oldName", business.getName());
            metadata.put("newName", updatedBusiness.getName());
        }
        if (request.getBusinessType() != null && !business.getBusinessType().equals(request.getBusinessType())) {
            metadata.put("oldBusinessType", business.getBusinessType().name());
            metadata.put("newBusinessType", request.getBusinessType().name());
        }

        auditLogService.log(
                currentUser,
                AuditAction.BUSINESS_UPDATED,
                "Business",
                id,
                String.format("매장 정보 수정: %s", updatedBusiness.getName()),
                metadata
        );

        log.info("Business updated: id={}", id);

        return getBusinessWithSettings(id);
    }

    /**
     * 매장 Settings 수정
     */
    @Transactional
    public BusinessResponse updateBusinessSettings(Long id, BusinessSettingsUpdateRequest request, User currentUser) {
        if (!businessRepository.existsById(id)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        // 권한 체크
        if (!currentUser.canAccessBusiness(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ACCESS_DENIED);
        }

        BusinessSettings existing = businessSettingsRepository.findByBusinessId(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND,
                        "Settings를 찾을 수 없습니다"));

        // 업데이트할 필드만 변경 (null이 아닌 필드만 업데이트)
        BusinessSettings updated = BusinessSettings.builder()
                .id(existing.getId())
                .businessId(id)
                .bookingInterval(request.getBookingInterval() != null ? request.getBookingInterval() : existing.getBookingInterval())
                .autoConfirm(request.getAutoConfirm() != null ? request.getAutoConfirm() : existing.getAutoConfirm())
                .allowOnlineBooking(request.getAllowOnlineBooking() != null ? request.getAllowOnlineBooking() : existing.getAllowOnlineBooking())
                .maxAdvanceBookingDays(request.getMaxAdvanceBookingDays() != null ? request.getMaxAdvanceBookingDays() : existing.getMaxAdvanceBookingDays())
                .minAdvanceBookingHours(request.getMinAdvanceBookingHours() != null ? request.getMinAdvanceBookingHours() : existing.getMinAdvanceBookingHours())
                .sendConfirmationSms(request.getSendConfirmationSms() != null ? request.getSendConfirmationSms() : existing.getSendConfirmationSms())
                .sendReminderSms(request.getSendReminderSms() != null ? request.getSendReminderSms() : existing.getSendReminderSms())
                .reminderHoursBefore(request.getReminderHoursBefore() != null ? request.getReminderHoursBefore() : existing.getReminderHoursBefore())
                .sendCancelSms(request.getSendCancelSms() != null ? request.getSendCancelSms() : existing.getSendCancelSms())
                .kakaoChannelId(request.getKakaoChannelId() != null ? request.getKakaoChannelId() : existing.getKakaoChannelId())
                .kakaoApiKey(request.getKakaoApiKey() != null ? request.getKakaoApiKey() : existing.getKakaoApiKey())
                .kakaoEnabled(request.getKakaoEnabled() != null ? request.getKakaoEnabled() : existing.getKakaoEnabled())
                .kakaoSenderId(existing.getKakaoSenderId())
                .kakaoAlimtalkTriggers(existing.getKakaoAlimtalkTriggers())
                .kakaoVerifiedAt(existing.getKakaoVerifiedAt())
                .paymentMethods(request.getPaymentMethods() != null ? request.getPaymentMethods() : existing.getPaymentMethods())
                .requireDeposit(request.getRequireDeposit() != null ? request.getRequireDeposit() : existing.getRequireDeposit())
                .depositAmount(request.getDepositAmount() != null ? request.getDepositAmount() : existing.getDepositAmount())
                .allowCancellation(request.getAllowCancellation() != null ? request.getAllowCancellation() : existing.getAllowCancellation())
                .cancelDeadlineHours(request.getCancelDeadlineHours() != null ? request.getCancelDeadlineHours() : existing.getCancelDeadlineHours())
                .noShowPenaltyEnabled(request.getNoShowPenaltyEnabled() != null ? request.getNoShowPenaltyEnabled() : existing.getNoShowPenaltyEnabled())
                .timezone(request.getTimezone() != null ? request.getTimezone() : existing.getTimezone())
                .language(request.getLanguage() != null ? request.getLanguage() : existing.getLanguage())
                .regularThreshold(existing.getRegularThreshold())
                .vipThreshold(existing.getVipThreshold())
                .vipBenefitDescription(existing.getVipBenefitDescription())
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
    public void deleteBusiness(Long id, User currentUser) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        // 권한 체크
        if (!currentUser.canAccessBusiness(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ACCESS_DENIED);
        }

        // 감사 로그 기록 (삭제 전에 기록)
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("businessName", business.getName());
        metadata.put("businessType", business.getBusinessType().name());
        metadata.put("ownerId", business.getOwnerId());

        auditLogService.log(
                currentUser,
                AuditAction.BUSINESS_DELETED,
                "Business",
                id,
                String.format("매장 삭제: %s", business.getName()),
                metadata
        );

        businessSettingsRepository.deleteByBusinessId(id);
        businessRepository.delete(id);

        log.info("Business deleted: id={}", id);
    }

    /**
     * 매장 상태 변경
     */
    @Transactional
    public BusinessResponse changeBusinessStatus(Long id, BusinessStatus status, User currentUser) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        // 권한 체크
        if (!currentUser.canAccessBusiness(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ACCESS_DENIED);
        }

        BusinessStatus oldStatus = business.getStatus();

        businessRepository.updateStatus(id, status);

        // 감사 로그 기록
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("businessName", business.getName());
        metadata.put("oldStatus", oldStatus.name());
        metadata.put("newStatus", status.name());

        auditLogService.log(
                currentUser,
                AuditAction.BUSINESS_STATUS_CHANGED,
                "Business",
                id,
                String.format("매장 상태 변경: %s (%s → %s)", business.getName(), oldStatus.getDescription(), status.getDescription()),
                metadata
        );

        log.info("Business status changed: id={}, status={}", id, status);

        return getBusinessWithSettings(id);
    }

    /**
     * 매장 프로필 이미지 업로드
     */
    @Transactional
    public ProfileImageResponse uploadProfileImage(Long id, MultipartFile file, User currentUser) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        if (!currentUser.canAccessBusiness(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ACCESS_DENIED);
        }

        // 기존 이미지 삭제
        if (business.getProfileImageUrl() != null) {
            fileStorageService.delete(business.getProfileImageUrl());
        }

        // 새 이미지 저장
        String imageUrl = fileStorageService.store(file, "businesses");

        // DB 업데이트
        businessRepository.updateProfileImageUrl(id, imageUrl);

        log.info("Business profile image uploaded: businessId={}, url={}", id, imageUrl);
        return new ProfileImageResponse(imageUrl);
    }

    /**
     * 매장 프로필 이미지 삭제
     */
    @Transactional
    public void deleteProfileImage(Long id, User currentUser) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        if (!currentUser.canAccessBusiness(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ACCESS_DENIED);
        }

        if (business.getProfileImageUrl() != null) {
            fileStorageService.delete(business.getProfileImageUrl());
            businessRepository.updateProfileImageUrl(id, null);
            log.info("Business profile image deleted: businessId={}", id);
        }
    }

    // === Private Methods ===

    /**
     * 유니크 slug 자동 생성
     * 형식: biz-{uuid8} (예: biz-a1b2c3d4)
     */
    private String generateUniqueSlug() {
        for (int i = 0; i < 5; i++) {
            String slug = "biz-" + UUID.randomUUID().toString().substring(0, 8);
            if (!businessRepository.existsBySlug(slug)) {
                return slug;
            }
        }
        // 극히 드문 충돌 시 더 긴 UUID 사용
        return "biz-" + UUID.randomUUID().toString().substring(0, 12);
    }

    // ========================================
    // 고객 등급 설정
    // ========================================

    /**
     * 고객 등급 임계값 조회
     */
    public CustomerTierSettingsResponse getCustomerTierSettings(Long businessId, User currentUser) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }
        if (!currentUser.canAccessBusiness(businessId)) {
            throw new BusinessException(ErrorCode.BUSINESS_ACCESS_DENIED);
        }

        BusinessSettings settings = businessSettingsRepository.findByBusinessId(businessId)
                .orElse(null);

        if (settings == null) {
            return CustomerTierSettingsResponse.defaults();
        }

        return CustomerTierSettingsResponse.builder()
                .regularThreshold(settings.getRegularThresholdValue())
                .vipThreshold(settings.getVipThresholdValue())
                .vipSpendThreshold(settings.getVipSpendThresholdValue())
                .vipBenefitDescription(settings.getVipBenefitDescription() != null ?
                        settings.getVipBenefitDescription() : "")
                .build();
    }

    /**
     * 고객 등급 임계값 설정
     */
    @Transactional
    public CustomerTierSettingsResponse updateCustomerTierSettings(Long businessId,
                                                                    CustomerTierSettingsRequest request,
                                                                    User currentUser) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }
        if (!currentUser.canAccessBusiness(businessId)) {
            throw new BusinessException(ErrorCode.BUSINESS_ACCESS_DENIED);
        }

        // VIP 임계값은 단골 임계값보다 커야 함
        int regularThreshold = request.getRegularThreshold() != null ? request.getRegularThreshold() : 3;
        int vipThreshold = request.getVipThreshold() != null ? request.getVipThreshold() : 10;

        if (vipThreshold <= regularThreshold) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "VIP 임계값은 단골 임계값보다 커야 합니다");
        }

        BusinessSettings existing = businessSettingsRepository.findByBusinessId(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND,
                        "Settings를 찾을 수 없습니다"));

        BusinessSettings updated = BusinessSettings.builder()
                .id(existing.getId())
                .businessId(businessId)
                .bookingInterval(existing.getBookingInterval())
                .autoConfirm(existing.getAutoConfirm())
                .allowOnlineBooking(existing.getAllowOnlineBooking())
                .maxAdvanceBookingDays(existing.getMaxAdvanceBookingDays())
                .minAdvanceBookingHours(existing.getMinAdvanceBookingHours())
                .sendConfirmationSms(existing.getSendConfirmationSms())
                .sendReminderSms(existing.getSendReminderSms())
                .reminderHoursBefore(existing.getReminderHoursBefore())
                .sendCancelSms(existing.getSendCancelSms())
                .kakaoChannelId(existing.getKakaoChannelId())
                .kakaoApiKey(existing.getKakaoApiKey())
                .kakaoEnabled(existing.getKakaoEnabled())
                .kakaoSenderId(existing.getKakaoSenderId())
                .kakaoAlimtalkTriggers(existing.getKakaoAlimtalkTriggers())
                .kakaoVerifiedAt(existing.getKakaoVerifiedAt())
                .paymentMethods(existing.getPaymentMethods())
                .requireDeposit(existing.getRequireDeposit())
                .depositAmount(existing.getDepositAmount())
                .allowCancellation(existing.getAllowCancellation())
                .cancelDeadlineHours(existing.getCancelDeadlineHours())
                .noShowPenaltyEnabled(existing.getNoShowPenaltyEnabled())
                .timezone(existing.getTimezone())
                .language(existing.getLanguage())
                .regularThreshold(regularThreshold)
                .vipThreshold(vipThreshold)
                .vipSpendThreshold(request.getVipSpendThreshold() != null ?
                        request.getVipSpendThreshold() : existing.getVipSpendThresholdValue())
                .vipBenefitDescription(request.getVipBenefitDescription() != null ?
                        request.getVipBenefitDescription() : existing.getVipBenefitDescription())
                .onboardingCompleted(existing.getOnboardingCompleted())
                .onboardingSkipped(existing.getOnboardingSkipped())
                .onboardingStepService(existing.getOnboardingStepService())
                .onboardingStepStaff(existing.getOnboardingStepStaff())
                .onboardingStepReservation(existing.getOnboardingStepReservation())
                .createdAt(existing.getCreatedAt())
                .build();

        businessSettingsRepository.update(updated);

        log.info("Customer tier settings updated: businessId={}, regular={}, vip={}",
                businessId, regularThreshold, vipThreshold);

        return CustomerTierSettingsResponse.builder()
                .regularThreshold(regularThreshold)
                .vipThreshold(vipThreshold)
                .vipSpendThreshold(request.getVipSpendThreshold() != null ?
                        request.getVipSpendThreshold() : existing.getVipSpendThresholdValue())
                .vipBenefitDescription(request.getVipBenefitDescription() != null ?
                        request.getVipBenefitDescription() : existing.getVipBenefitDescription())
                .build();
    }

    // ========================================
    // 카카오 알림톡 설정
    // ========================================

    /**
     * 카카오 알림톡 설정 조회
     */
    public KakaoAlimtalkSettingsResponse getKakaoAlimtalkSettings(Long businessId, User currentUser) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }
        if (!currentUser.canAccessBusiness(businessId)) {
            throw new BusinessException(ErrorCode.BUSINESS_ACCESS_DENIED);
        }

        BusinessSettings settings = businessSettingsRepository.findByBusinessId(businessId)
                .orElse(null);

        if (settings == null) {
            return KakaoAlimtalkSettingsResponse.defaults();
        }

        return buildKakaoAlimtalkResponse(settings);
    }

    /**
     * 카카오 알림톡 설정 수정
     */
    @Transactional
    public KakaoAlimtalkSettingsResponse updateKakaoAlimtalkSettings(Long businessId,
                                                                      KakaoAlimtalkSettingsRequest request,
                                                                      User currentUser) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }
        if (!currentUser.canAccessBusiness(businessId)) {
            throw new BusinessException(ErrorCode.BUSINESS_ACCESS_DENIED);
        }

        BusinessSettings existing = businessSettingsRepository.findByBusinessId(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND,
                        "Settings를 찾을 수 없습니다"));

        // 활성화 시 채널 ID 필수 검증
        boolean enabled = request.getEnabled() != null ? request.getEnabled() : "Y".equals(existing.getKakaoEnabled());
        if (enabled) {
            String channelId = request.getChannelId() != null ? request.getChannelId() : existing.getKakaoChannelId();
            if (channelId == null || channelId.isBlank()) {
                throw new BusinessException(ErrorCode.KAKAO_CHANNEL_VERIFICATION_FAILED,
                        "카카오 알림톡을 활성화하려면 채널 ID가 필요합니다");
            }
        }

        // 트리거 설정 빌드
        Map<String, Object> triggers = buildTriggersMap(request, existing);

        String kakaoEnabled = enabled ? "Y" : "N";
        String channelId = request.getChannelId() != null ? request.getChannelId() : existing.getKakaoChannelId();
        String senderId = request.getSenderId() != null ? request.getSenderId() : existing.getKakaoSenderId();

        businessSettingsRepository.updateKakaoAlimtalkSettings(
                businessId,
                kakaoEnabled,
                channelId,
                senderId,
                triggers,
                existing.getKakaoVerifiedAt()
        );

        log.info("Kakao alimtalk settings updated: businessId={}, enabled={}", businessId, kakaoEnabled);

        // 업데이트된 설정 다시 조회하여 반환
        BusinessSettings updated = businessSettingsRepository.findByBusinessId(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        return buildKakaoAlimtalkResponse(updated);
    }

    private KakaoAlimtalkSettingsResponse buildKakaoAlimtalkResponse(BusinessSettings settings) {
        Map<String, Object> triggersMap = settings.getKakaoAlimtalkTriggers();

        KakaoAlimtalkSettingsResponse.AlimtalkTriggers triggers;
        if (triggersMap != null && !triggersMap.isEmpty()) {
            triggers = KakaoAlimtalkSettingsResponse.AlimtalkTriggers.builder()
                    .onReservationCreated(getBooleanValue(triggersMap, "onReservationCreated", true))
                    .onReservationConfirmed(getBooleanValue(triggersMap, "onReservationConfirmed", true))
                    .onReservationCancelled(getBooleanValue(triggersMap, "onReservationCancelled", true))
                    .onReservationReminder(getBooleanValue(triggersMap, "onReservationReminder", true))
                    .reminderHoursBefore(getIntValue(triggersMap, "reminderHoursBefore", 24))
                    .build();
        } else {
            triggers = KakaoAlimtalkSettingsResponse.AlimtalkTriggers.builder()
                    .onReservationCreated(true)
                    .onReservationConfirmed(true)
                    .onReservationCancelled(true)
                    .onReservationReminder(true)
                    .reminderHoursBefore(24)
                    .build();
        }

        return KakaoAlimtalkSettingsResponse.builder()
                .enabled("Y".equals(settings.getKakaoEnabled()))
                .channelId(settings.getKakaoChannelId())
                .senderId(settings.getKakaoSenderId())
                .triggers(triggers)
                .verifiedAt(settings.getKakaoVerifiedAt())
                .build();
    }

    private Map<String, Object> buildTriggersMap(KakaoAlimtalkSettingsRequest request, BusinessSettings existing) {
        Map<String, Object> existingTriggers = existing.getKakaoAlimtalkTriggers();
        Map<String, Object> triggers = new HashMap<>();

        if (request.getTriggers() != null) {
            KakaoAlimtalkSettingsRequest.AlimtalkTriggers reqTriggers = request.getTriggers();
            triggers.put("onReservationCreated",
                    reqTriggers.getOnReservationCreated() != null ? reqTriggers.getOnReservationCreated()
                            : getBooleanValue(existingTriggers, "onReservationCreated", true));
            triggers.put("onReservationConfirmed",
                    reqTriggers.getOnReservationConfirmed() != null ? reqTriggers.getOnReservationConfirmed()
                            : getBooleanValue(existingTriggers, "onReservationConfirmed", true));
            triggers.put("onReservationCancelled",
                    reqTriggers.getOnReservationCancelled() != null ? reqTriggers.getOnReservationCancelled()
                            : getBooleanValue(existingTriggers, "onReservationCancelled", true));
            triggers.put("onReservationReminder",
                    reqTriggers.getOnReservationReminder() != null ? reqTriggers.getOnReservationReminder()
                            : getBooleanValue(existingTriggers, "onReservationReminder", true));
            triggers.put("reminderHoursBefore",
                    reqTriggers.getReminderHoursBefore() != null ? reqTriggers.getReminderHoursBefore()
                            : getIntValue(existingTriggers, "reminderHoursBefore", 24));
        } else if (existingTriggers != null) {
            triggers.putAll(existingTriggers);
        } else {
            // 기본값
            triggers.put("onReservationCreated", true);
            triggers.put("onReservationConfirmed", true);
            triggers.put("onReservationCancelled", true);
            triggers.put("onReservationReminder", true);
            triggers.put("reminderHoursBefore", 24);
        }

        return triggers;
    }

    private boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        if (map == null || !map.containsKey(key)) return defaultValue;
        Object value = map.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        return defaultValue;
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        if (map == null || !map.containsKey(key)) return defaultValue;
        Object value = map.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        return defaultValue;
    }

    private BusinessResponse getBusinessWithSettings(Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        BusinessSettings settings = businessSettingsRepository
                .findByBusinessId(businessId)
                .orElseGet(() -> businessSettingsService.createDefaultSettings(businessId));

        return BusinessResponse.from(business, settings);
    }

}