package io.moer.booking.domain.subscription.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.BillingCycle;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.SubscriptionPlan;
import io.moer.booking.domain.business.SubscriptionStatus;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.subscription.dto.SubscriptionInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final BusinessRepository businessRepository;

    /**
     * 구독 정보 조회
     */
    public SubscriptionInfoResponse getSubscriptionInfo(Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        return SubscriptionInfoResponse.from(business);
    }

    /**
     * 플랜 변경
     */
    @Transactional
    public SubscriptionInfoResponse changePlan(Long businessId, SubscriptionPlan newPlan, BillingCycle billingCycle) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        SubscriptionPlan currentPlan = business.getSubscriptionPlan();
        BillingCycle currentCycle = business.getBillingCycle();

        // 1. 동일한 플랜 + 동일 billingCycle 체크
        if (currentPlan == newPlan && currentCycle == billingCycle) {
            throw new BusinessException(ErrorCode.SAME_PLAN);
        }

        // 2. 다운그레이드 시 현재 사용량 체크
        if (isDowngrade(currentPlan, newPlan)) {
            validateDowngrade(business, newPlan);
        }

        // 3. 플랜 변경 (Builder 패턴 사용 - 불변 객체이므로 재생성)
        Business updatedBusiness = Business.builder()
                .id(business.getId())
                .ownerId(business.getOwnerId())
                .name(business.getName())
                .slug(business.getSlug())
                .businessType(business.getBusinessType())
                .phone(business.getPhone())
                .address(business.getAddress())
                .description(business.getDescription())
                .businessHours(business.getBusinessHours())
                .status(business.getStatus())
                .dailyRevenueGoal(business.getDailyRevenueGoal())
                .monthlyRevenueGoal(business.getMonthlyRevenueGoal())
                .monthlyNewCustomerGoal(business.getMonthlyNewCustomerGoal())
                .subscriptionPlan(newPlan)  // 새 플랜
                .billingCycle(billingCycle)  // 새 결제 주기
                .subscriptionStatus(business.getSubscriptionStatus())
                .trialStartedAt(business.getTrialStartedAt())
                .trialEndsAt(business.getTrialEndsAt())
                .subscriptionStartedAt(business.getSubscriptionStartedAt())
                .nextBillingDate(business.getNextBillingDate())
                .currentStaffCount(business.getCurrentStaffCount())
                .currentMonthReservationCount(business.getCurrentMonthReservationCount())
                .createdAt(business.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        businessRepository.update(updatedBusiness);

        log.info("Plan changed: businessId={}, {} -> {}, billingCycle={}", businessId, currentPlan, newPlan, billingCycle);

        return SubscriptionInfoResponse.from(updatedBusiness);
    }

    /**
     * 구독 취소
     * - 상태를 CANCELED로 변경
     * - nextBillingDate를 보존하여 잔여 기간 안내에 사용 (FE에서 expiresAt으로 활용)
     * - 취소 후에도 nextBillingDate까지 서비스 이용 가능
     *
     * @return 취소 후 구독 정보 (잔여 기간 포함)
     */
    @Transactional
    public SubscriptionInfoResponse cancelSubscription(Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        // 이미 취소된 구독
        if (business.getSubscriptionStatus() == SubscriptionStatus.CANCELED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 취소된 구독입니다");
        }

        // 구독 상태를 CANCELED로 변경 (nextBillingDate 보존하여 잔여 기간 안내)
        Business updatedBusiness = Business.builder()
                .id(business.getId())
                .ownerId(business.getOwnerId())
                .name(business.getName())
                .slug(business.getSlug())
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
                .subscriptionStatus(SubscriptionStatus.CANCELED)  // 취소 상태
                .trialStartedAt(business.getTrialStartedAt())
                .trialEndsAt(business.getTrialEndsAt())
                .subscriptionStartedAt(business.getSubscriptionStartedAt())
                .nextBillingDate(business.getNextBillingDate())  // 보존: 잔여 기간 안내용 (expiresAt)
                .currentStaffCount(business.getCurrentStaffCount())
                .currentMonthReservationCount(business.getCurrentMonthReservationCount())
                .createdAt(business.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        businessRepository.update(updatedBusiness);

        log.info("Subscription canceled: businessId={}, expiresAt={}",
                businessId, business.getNextBillingDate());

        return SubscriptionInfoResponse.from(updatedBusiness);
    }

    /**
     * 결제 완료 시 구독 활성화
     * PaymentService에서 결제 완료 후 호출
     *
     * @param businessId 매장 ID
     * @param newPlan 새 플랜
     * @param billingCycle 결제 주기
     * @param billingEndDate 다음 결제일
     */
    @Transactional
    public void activateSubscriptionAfterPayment(Long businessId, SubscriptionPlan newPlan,
                                                  BillingCycle billingCycle, LocalDateTime billingEndDate) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        // 1. 체험판 종료 로그
        if (business.getSubscriptionStatus() == SubscriptionStatus.TRIAL) {
            log.info("체험판 종료: businessId={}, 이전 플랜={}", businessId, business.getSubscriptionPlan());
        }

        // 2. 유료 구독 활성화
        LocalDateTime now = LocalDateTime.now();
        Business updatedBusiness = Business.builder()
                .id(business.getId())
                .ownerId(business.getOwnerId())
                .name(business.getName())
                .slug(business.getSlug())
                .businessType(business.getBusinessType())
                .phone(business.getPhone())
                .address(business.getAddress())
                .description(business.getDescription())
                .businessHours(business.getBusinessHours())
                .status(business.getStatus())
                .dailyRevenueGoal(business.getDailyRevenueGoal())
                .monthlyRevenueGoal(business.getMonthlyRevenueGoal())
                .monthlyNewCustomerGoal(business.getMonthlyNewCustomerGoal())
                .subscriptionPlan(newPlan)
                .billingCycle(billingCycle)
                .subscriptionStatus(SubscriptionStatus.ACTIVE) // 체험판 → 활성
                .trialStartedAt(business.getTrialStartedAt())
                .trialEndsAt(business.getTrialEndsAt())
                .subscriptionStartedAt(now) // 유료 구독 시작
                .nextBillingDate(billingEndDate) // 다음 결제일
                .currentStaffCount(business.getCurrentStaffCount())
                .currentMonthReservationCount(business.getCurrentMonthReservationCount())
                .createdAt(business.getCreatedAt())
                .updatedAt(now)
                .build();

        businessRepository.update(updatedBusiness);

        log.info("유료 구독 활성화: businessId={}, plan={}, billingCycle={}, nextBillingDate={}",
                businessId, newPlan, billingCycle, billingEndDate);
    }

    /**
     * 기간 연장 결제 후 구독 갱신
     * 기존 구독의 nextBillingDate만 업데이트 (subscriptionStartedAt은 유지)
     */
    @Transactional
    public void extendSubscriptionAfterPayment(Long businessId, LocalDateTime newBillingEndDate) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        Business updatedBusiness = Business.builder()
                .id(business.getId())
                .ownerId(business.getOwnerId())
                .name(business.getName())
                .slug(business.getSlug())
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
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .trialStartedAt(business.getTrialStartedAt())
                .trialEndsAt(business.getTrialEndsAt())
                .subscriptionStartedAt(business.getSubscriptionStartedAt()) // 기존 시작일 유지
                .nextBillingDate(newBillingEndDate) // 연장된 종료일
                .currentStaffCount(business.getCurrentStaffCount())
                .currentMonthReservationCount(business.getCurrentMonthReservationCount())
                .createdAt(business.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        businessRepository.update(updatedBusiness);

        log.info("구독 기간 연장: businessId={}, newBillingEndDate={}", businessId, newBillingEndDate);
    }

    // ========================================
    // Private 헬퍼 메서드
    // ========================================

    /**
     * 다운그레이드 여부 확인
     */
    private boolean isDowngrade(SubscriptionPlan current, SubscriptionPlan target) {
        int currentOrder = getPlanOrder(current);
        int targetOrder = getPlanOrder(target);
        return targetOrder < currentOrder;
    }

    /**
     * 플랜 순서 (FREE < BASIC)
     */
    private int getPlanOrder(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE -> 0;
            case BASIC -> 1;
        };
    }

    /**
     * 다운그레이드 가능 여부 검증
     */
    private void validateDowngrade(Business business, SubscriptionPlan newPlan) {
        // 직원 수 체크
        int currentStaffCount = business.getCurrentStaffCount();
        int newMaxStaff = newPlan.getMaxStaff();
        if (newMaxStaff != -1 && currentStaffCount > newMaxStaff) {
            throw new BusinessException(
                    ErrorCode.DOWNGRADE_NOT_ALLOWED,
                    String.format("현재 직원 수(%d명)가 새 플랜의 제한(%d명)을 초과합니다",
                            currentStaffCount, newMaxStaff)
            );
        }

        // 월간 예약 수는 다운그레이드 시 체크하지 않음 (이미 생성된 예약은 유지)
    }
}
