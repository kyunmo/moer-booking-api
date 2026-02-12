package io.moer.booking.domain.coupon.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.coupon.Coupon;
import io.moer.booking.domain.coupon.CouponStatus;
import io.moer.booking.domain.coupon.CouponUsage;
import io.moer.booking.domain.coupon.dto.CouponCreateRequest;
import io.moer.booking.domain.coupon.dto.CouponResponse;
import io.moer.booking.domain.coupon.dto.CouponSearchCondition;
import io.moer.booking.domain.coupon.repository.CouponRepository;
import io.moer.booking.domain.coupon.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    /**
     * 쿠폰 생성
     */
    @Transactional
    public CouponResponse createCoupon(Long businessId, CouponCreateRequest request) {
        // 1. 쿠폰 코드 중복 확인
        if (couponRepository.existsByCode(request.getCode())) {
            throw new BusinessException(
                ErrorCode.COUPON_DUPLICATE_CODE,
                "이미 존재하는 쿠폰 코드입니다: " + request.getCode()
            );
        }

        // 2. 할인 금액/비율 검증
        validateDiscountValues(request);

        // 3. Coupon 생성
        Coupon coupon = Coupon.builder()
            .businessId(businessId)
            .code(request.getCode())
            .name(request.getName())
            .description(request.getDescription())
            .couponType(request.getCouponType())
            .discountAmount(request.getDiscountAmount())
            .discountPercentage(request.getDiscountPercentage())
            .maxDiscountAmount(request.getMaxDiscountAmount())
            .minOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : 0)
            .maxUsageCount(request.getMaxUsageCount())
            .currentUsageCount(0)
            .validFrom(request.getValidFrom())
            .validUntil(request.getValidUntil())
            .status(CouponStatus.ACTIVE)
            .build();

        couponRepository.save(coupon);
        log.info("쿠폰 생성: couponId={}, code={}, type={}",
            coupon.getId(), coupon.getCode(), coupon.getCouponType());

        return CouponResponse.from(coupon);
    }

    /**
     * 쿠폰 조회 (단건)
     */
    public CouponResponse getCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new EntityNotFoundException(ErrorCode.COUPON_NOT_FOUND));
        return CouponResponse.from(coupon);
    }

    /**
     * 쿠폰 조회 (코드)
     */
    public CouponResponse getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCode(code)
            .orElseThrow(() -> new EntityNotFoundException(
                ErrorCode.COUPON_NOT_FOUND,
                "존재하지 않는 쿠폰 코드입니다: " + code
            ));
        return CouponResponse.from(coupon);
    }

    /**
     * 쿠폰 목록 조회
     */
    public List<CouponResponse> getCouponList(CouponSearchCondition condition) {
        return couponRepository.findByCondition(condition)
            .stream()
            .map(CouponResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * 쿠폰 검증 (사용 가능 여부)
     */
    public CouponResponse validateCoupon(String code, Long userId, int orderAmount) {
        // 1. 쿠폰 조회
        Coupon coupon = couponRepository.findByCode(code)
            .orElseThrow(() -> new EntityNotFoundException(
                ErrorCode.COUPON_NOT_FOUND,
                "존재하지 않는 쿠폰 코드입니다: " + code
            ));

        // 2. 쿠폰 사용 가능 여부 검증
        coupon.validateUsage(orderAmount);

        // 3. 사용자가 이미 사용했는지 확인 (중복 사용 방지)
        boolean alreadyUsed = couponUsageRepository.existsByUserIdAndCouponId(userId, coupon.getId());
        if (alreadyUsed) {
            throw new BusinessException(
                ErrorCode.COUPON_ALREADY_USED,
                "이미 사용한 쿠폰입니다"
            );
        }

        log.info("쿠폰 검증 성공: code={}, userId={}, orderAmount={}", code, userId, orderAmount);
        return CouponResponse.from(coupon);
    }

    /**
     * 쿠폰 사용 (CouponUsage 생성)
     * PaymentService에서 호출
     */
    @Transactional
    public CouponUsage useCoupon(Long couponId, Long userId, Long paymentId, int orderAmount) {
        // 1. 쿠폰 조회
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new EntityNotFoundException(ErrorCode.COUPON_NOT_FOUND));

        // 2. 쿠폰 사용 가능 여부 검증
        coupon.validateUsage(orderAmount);

        // 3. 할인 금액 계산
        int discountAmount = coupon.calculateDiscount(orderAmount);

        // 4. CouponUsage 생성
        CouponUsage usage = CouponUsage.builder()
            .couponId(couponId)
            .userId(userId)
            .paymentId(paymentId)
            .discountAmount(discountAmount)
            .usedAt(LocalDateTime.now())
            .canceled("N")
            .build();

        couponUsageRepository.save(usage);

        // 5. 쿠폰 사용 횟수 증가
        couponRepository.incrementUsageCount(couponId);

        log.info("쿠폰 사용: usageId={}, couponId={}, userId={}, discountAmount={}",
            usage.getId(), couponId, userId, discountAmount);

        return usage;
    }

    /**
     * 쿠폰 사용 취소
     * 결제 취소/환불 시 호출
     */
    @Transactional
    public void cancelCouponUsage(Long paymentId) {
        // 1. CouponUsage 조회
        CouponUsage usage = couponUsageRepository.findByPaymentId(paymentId)
            .orElse(null);

        if (usage == null || usage.isCanceled()) {
            return; // 쿠폰을 사용하지 않았거나 이미 취소됨
        }

        // 2. CouponUsage 취소 처리
        CouponUsage updatedUsage = CouponUsage.builder()
            .id(usage.getId())
            .couponId(usage.getCouponId())
            .userId(usage.getUserId())
            .paymentId(usage.getPaymentId())
            .discountAmount(usage.getDiscountAmount())
            .usedAt(usage.getUsedAt())
            .canceled("Y")
            .canceledAt(LocalDateTime.now())
            .build();

        couponUsageRepository.update(updatedUsage);

        // 3. 쿠폰 사용 횟수 감소
        couponRepository.decrementUsageCount(usage.getCouponId());

        log.info("쿠폰 사용 취소: usageId={}, couponId={}, paymentId={}",
            usage.getId(), usage.getCouponId(), paymentId);
    }

    /**
     * 할인 금액 검증
     */
    private void validateDiscountValues(CouponCreateRequest request) {
        switch (request.getCouponType()) {
            case FIXED_AMOUNT:
                if (request.getDiscountAmount() == null || request.getDiscountAmount() <= 0) {
                    throw new BusinessException(
                        ErrorCode.INVALID_COUPON_CODE,
                        "정액 할인 쿠폰은 할인 금액이 필요합니다"
                    );
                }
                break;
            case PERCENTAGE:
                if (request.getDiscountPercentage() == null ||
                    request.getDiscountPercentage() <= 0 ||
                    request.getDiscountPercentage() > 100) {
                    throw new BusinessException(
                        ErrorCode.INVALID_COUPON_CODE,
                        "정률 할인 쿠폰은 1~100 사이의 할인 비율이 필요합니다"
                    );
                }
                break;
        }
    }
}
