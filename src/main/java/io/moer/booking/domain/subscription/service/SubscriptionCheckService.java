package io.moer.booking.domain.subscription.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.SubscriptionStatus;
import io.moer.booking.domain.business.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 구독/체험판 상태 체크 서비스
 *
 * <p>프리미엄 기능 접근 제한 및 체험판/구독 상태 검증을 담당한다.</p>
 *
 * <h3>에러 코드 매핑:</h3>
 * <ul>
 *   <li>TR001 (TRIAL_EXPIRED): 체험판 만료 후 유료 기능 접근 시</li>
 *   <li>TR002 (TRIAL_FEATURE_RESTRICTED): 무료 버전에서 유료 전용 기능 접근 시</li>
 *   <li>TR003 (UPGRADE_REQUIRED): 구독 만료/취소/정지 상태에서 접근 시</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionCheckService {

    private final BusinessRepository businessRepository;

    /**
     * 기본 서비스 사용 가능 여부 체크
     *
     * <p>TRIAL 또는 ACTIVE 상태가 아니면 에러를 throw한다.</p>
     *
     * @param businessId 매장 ID
     * @throws BusinessException TR001 (체험판 만료), SU002 (구독 만료/취소/정지)
     */
    public void checkServiceAccess(Long businessId) {
        Business business = getBusiness(businessId);

        if (business.canUseService()) {
            return; // TRIAL 또는 ACTIVE → 사용 가능
        }

        SubscriptionStatus status = business.getSubscriptionStatus();

        // 체험판이었다가 만료된 경우
        if (status == SubscriptionStatus.EXPIRED && business.getTrialEndsAt() != null
                && business.getSubscriptionStartedAt() == null) {
            throw new BusinessException(ErrorCode.TRIAL_EXPIRED,
                    "체험 기간이 종료되었습니다. 유료 플랜으로 업그레이드해 주세요.");
        }

        // 그 외 만료/취소/정지
        throw new BusinessException(ErrorCode.SUBSCRIPTION_EXPIRED,
                "구독이 만료되었습니다. 플랜을 갱신해 주세요.");
    }

    /**
     * 유료 전용 기능 접근 체크 (프리미엄 기능)
     *
     * <p>접근 허용 조건:</p>
     * <ul>
     *   <li>TRIAL (활성 체험 기간) → 허용</li>
     *   <li>PAID + ACTIVE (유료 구독 활성) → 허용</li>
     * </ul>
     *
     * <p>접근 차단 조건:</p>
     * <ul>
     *   <li>FREE (EXPIRED) → TR002 차단</li>
     *   <li>구독 만료/취소/정지 → TR003 차단</li>
     * </ul>
     *
     * @param businessId 매장 ID
     * @throws BusinessException TR001, TR002, TR003
     */
    public void checkPremiumAccess(Long businessId) {
        Business business = getBusiness(businessId);
        SubscriptionStatus status = business.getSubscriptionStatus();

        // 1. 유료 플랜 + 활성 상태 → 통과
        if (business.isPaidPlan() && status == SubscriptionStatus.ACTIVE) {
            return;
        }

        // 2. 체험 기간 중 → 모든 기능 허용
        if (status == SubscriptionStatus.TRIAL) {
            return;
        }

        // 3. 체험판 만료 (EXPIRED + 유료 결제 이력 없음)
        if (status == SubscriptionStatus.EXPIRED && business.getSubscriptionStartedAt() == null) {
            throw new BusinessException(ErrorCode.TRIAL_EXPIRED,
                    "체험 기간이 종료되었습니다. 유료 플랜으로 업그레이드해 주세요.");
        }

        // 4. FREE 플랜 (무료 전환 후)
        if (business.isFreePlan()) {
            throw new BusinessException(ErrorCode.TRIAL_FEATURE_RESTRICTED,
                    "무료 버전에서는 사용할 수 없는 기능입니다. 유료 플랜으로 업그레이드해주세요.");
        }

        // 5. 그 외 (구독 만료/취소/정지 등)
        throw new BusinessException(ErrorCode.UPGRADE_REQUIRED,
                "유료 플랜 전용 기능입니다. 플랜을 업그레이드해 주세요.");
    }

    private Business getBusiness(Long businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));
    }
}
