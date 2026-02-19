package io.moer.booking.domain.subscription.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.SubscriptionPlan;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.service.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsageLimitService {

    private final BusinessRepository businessRepository;
    private final ServiceRepository serviceRepository;

    /**
     * 직원 추가 가능 여부 체크
     * @throws BusinessException STAFF_LIMIT_EXCEEDED
     */
    public void checkCanAddStaff(Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        if (!business.canAddStaff()) {
            SubscriptionPlan plan = business.getSubscriptionPlan();
            throw new BusinessException(
                    ErrorCode.STAFF_LIMIT_EXCEEDED,
                    String.format("직원 수 제한에 도달했습니다 (현재: %d명, 최대: %d명). 플랜을 업그레이드하세요.",
                            business.getCurrentStaffCount(), plan.getMaxStaff())
            );
        }
    }

    /**
     * 서비스 추가 가능 여부 체크
     * @throws BusinessException SL004 SERVICE_LIMIT_EXCEEDED
     */
    public void checkCanAddService(Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        SubscriptionPlan plan = business.getSubscriptionPlan();
        if (plan.getMaxServices() == -1) {
            return; // 무제한
        }

        long currentServiceCount = serviceRepository.countByBusinessId(businessId);
        if (!plan.canAddService((int) currentServiceCount)) {
            throw new BusinessException(
                    ErrorCode.SERVICE_LIMIT_EXCEEDED,
                    String.format("서비스 등록 수 제한에 도달했습니다 (현재: %d개, 최대: %d개). 플랜을 업그레이드하세요.",
                            currentServiceCount, plan.getMaxServices())
            );
        }
    }

    /**
     * 예약 생성 가능 여부 체크
     * @throws BusinessException RESERVATION_LIMIT_EXCEEDED
     */
    public void checkCanCreateReservation(Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        if (!business.canCreateReservation()) {
            SubscriptionPlan plan = business.getSubscriptionPlan();
            throw new BusinessException(
                    ErrorCode.RESERVATION_LIMIT_EXCEEDED,
                    String.format("월간 예약 수 제한에 도달했습니다 (현재: %d건, 최대: %d건). 플랜을 업그레이드하세요.",
                            business.getCurrentMonthReservationCount(), plan.getMaxMonthlyReservations())
            );
        }
    }

    /**
     * 직원 수 증가 (캐시 업데이트)
     */
    @Transactional
    public void incrementStaffCount(Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        int newCount = business.getCurrentStaffCount() + 1;
        updateStaffCount(business, newCount);

        log.debug("Staff count incremented: businessId={}, newCount={}", businessId, newCount);
    }

    /**
     * 직원 수 감소 (캐시 업데이트)
     */
    @Transactional
    public void decrementStaffCount(Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        int newCount = Math.max(0, business.getCurrentStaffCount() - 1);
        updateStaffCount(business, newCount);

        log.debug("Staff count decremented: businessId={}, newCount={}", businessId, newCount);
    }

    /**
     * 월간 예약 수 증가 (캐시 업데이트)
     */
    @Transactional
    public void incrementReservationCount(Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        int newCount = business.getCurrentMonthReservationCount() + 1;
        updateReservationCount(business, newCount);

        log.debug("Reservation count incremented: businessId={}, newCount={}", businessId, newCount);
    }

    /**
     * 월간 예약 수 감소 (캐시 업데이트)
     */
    @Transactional
    public void decrementReservationCount(Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        int newCount = Math.max(0, business.getCurrentMonthReservationCount() - 1);
        updateReservationCount(business, newCount);

        log.debug("Reservation count decremented: businessId={}, newCount={}", businessId, newCount);
    }

    // ========================================
    // Private 헬퍼 메서드
    // ========================================

    private void updateStaffCount(Business business, int newCount) {
        Business updated = Business.builder()
                .id(business.getId())
                .ownerId(business.getOwnerId())
                .name(business.getName())
                .businessType(business.getBusinessType())
                .phone(business.getPhone())
                .address(business.getAddress())
                .description(business.getDescription())
                .businessHours(business.getBusinessHours())
                .status(business.getStatus())
                .dailyRevenueGoal(business.getDailyRevenueGoal())
                .monthlyRevenueGoal(business.getMonthlyRevenueGoal())
                .monthlyNewCustomerGoal(business.getMonthlyNewCustomerGoal())
                .subscriptionPlan(business.getSubscriptionPlan())
                .billingCycle(business.getBillingCycle())
                .subscriptionStatus(business.getSubscriptionStatus())
                .trialStartedAt(business.getTrialStartedAt())
                .trialEndsAt(business.getTrialEndsAt())
                .subscriptionStartedAt(business.getSubscriptionStartedAt())
                .nextBillingDate(business.getNextBillingDate())
                .currentStaffCount(newCount)  // 업데이트
                .currentMonthReservationCount(business.getCurrentMonthReservationCount())
                .createdAt(business.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        businessRepository.update(updated);
    }

    private void updateReservationCount(Business business, int newCount) {
        Business updated = Business.builder()
                .id(business.getId())
                .ownerId(business.getOwnerId())
                .name(business.getName())
                .businessType(business.getBusinessType())
                .phone(business.getPhone())
                .address(business.getAddress())
                .description(business.getDescription())
                .businessHours(business.getBusinessHours())
                .status(business.getStatus())
                .dailyRevenueGoal(business.getDailyRevenueGoal())
                .monthlyRevenueGoal(business.getMonthlyRevenueGoal())
                .monthlyNewCustomerGoal(business.getMonthlyNewCustomerGoal())
                .subscriptionPlan(business.getSubscriptionPlan())
                .billingCycle(business.getBillingCycle())
                .subscriptionStatus(business.getSubscriptionStatus())
                .trialStartedAt(business.getTrialStartedAt())
                .trialEndsAt(business.getTrialEndsAt())
                .subscriptionStartedAt(business.getSubscriptionStartedAt())
                .nextBillingDate(business.getNextBillingDate())
                .currentStaffCount(business.getCurrentStaffCount())
                .currentMonthReservationCount(newCount)  // 업데이트
                .createdAt(business.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        businessRepository.update(updated);
    }
}
