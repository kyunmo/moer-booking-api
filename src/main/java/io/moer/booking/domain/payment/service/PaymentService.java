package io.moer.booking.domain.payment.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.BillingCycle;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.SubscriptionPlan;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.payment.Payment;
import io.moer.booking.domain.payment.PaymentStatus;
import io.moer.booking.domain.payment.dto.PaymentCreateRequest;
import io.moer.booking.domain.payment.dto.PaymentResponse;
import io.moer.booking.domain.payment.dto.PaymentSearchCondition;
import io.moer.booking.domain.payment.repository.PaymentRepository;
import io.moer.booking.domain.coupon.Coupon;
import io.moer.booking.domain.coupon.CouponUsage;
import io.moer.booking.domain.coupon.dto.CouponResponse;
import io.moer.booking.domain.coupon.repository.CouponRepository;
import io.moer.booking.domain.coupon.service.CouponService;
import io.moer.booking.domain.subscription.service.SubscriptionService;
import io.moer.booking.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 결제 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BusinessRepository businessRepository;
    private final FakePGService fakePGService;
    private final SubscriptionService subscriptionService;
    private final CouponService couponService;
    private final CouponRepository couponRepository;

    /**
     * 결제 생성 및 즉시 처리
     * PENDING 생성 → PG 호출 → COMPLETED/FAILED
     */
    @Transactional
    public PaymentResponse createAndProcessPayment(User user, PaymentCreateRequest request) {
        log.info("결제 시작: userId={}, plan={}, billingCycle={}", user.getId(), request.getPlan(), request.getBillingCycle());

        // 1. Business 조회
        Business business = businessRepository.findById(user.getBusinessId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        // 2. FREE 플랜은 결제 불가
        if (request.getPlan() == SubscriptionPlan.FREE) {
            throw new BusinessException(
                    ErrorCode.INVALID_PAYMENT_AMOUNT,
                    "FREE 플랜은 결제가 필요하지 않습니다"
            );
        }

        // 3. billingCycle 기본값 처리
        BillingCycle billingCycle = request.getBillingCycle() != null
                ? request.getBillingCycle()
                : BillingCycle.MONTHLY;

        // 4. 금액 계산 (결제 주기에 따라)
        int amount = request.getPlan().getPrice(billingCycle);
        int discountAmount = 0;
        Long couponId = null;
        CouponUsage couponUsage = null;

        // 5. 쿠폰 적용
        if (request.getCouponCode() != null && !request.getCouponCode().isEmpty()) {
            // 쿠폰 검증
            CouponResponse couponResponse = couponService.validateCoupon(
                request.getCouponCode(),
                user.getId(),
                amount
            );

            // 쿠폰 조회
            Coupon coupon = couponRepository.findByCode(request.getCouponCode())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.COUPON_NOT_FOUND));

            // 할인 금액 계산
            discountAmount = coupon.calculateDiscount(amount);
            couponId = coupon.getId();

            log.info("쿠폰 적용: code={}, originalAmount={}, discountAmount={}, finalAmount={}",
                request.getCouponCode(), amount, discountAmount, amount - discountAmount);
        }

        int finalAmount = amount - discountAmount;

        // 6. 청구 기간 계산 (결제 주기에 따라)
        LocalDate today = LocalDate.now();
        LocalDate billingEnd = billingCycle.isYearly() ? today.plusYears(1) : today.plusMonths(1);

        // 7. Payment 생성 (PENDING)
        Payment payment = Payment.builder()
                .businessId(business.getId())
                .couponId(couponId)
                .subscriptionPlan(request.getPlan())
                .billingCycle(billingCycle)
                .billingPeriodStart(today)
                .billingPeriodEnd(billingEnd)
                .amount(amount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .couponCode(request.getCouponCode())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .pgProvider("FAKE_PG")
                .build();

        paymentRepository.save(payment);
        log.info("결제 생성: paymentId={}, amount={}원, billingCycle={}", payment.getId(), finalAmount, billingCycle);

        // 8. PG 호출 (Fake)
        Map<String, Object> pgResponse = fakePGService.requestPayment(
                finalAmount,
                request.getPaymentMethod().name()
        );

        // 9. PG 응답 처리
        String pgStatus = (String) pgResponse.get("status");
        PaymentStatus newStatus = "COMPLETED".equals(pgStatus)
                ? PaymentStatus.COMPLETED
                : PaymentStatus.FAILED;

        Payment updatedPayment = Payment.builder()
                .id(payment.getId())
                .businessId(payment.getBusinessId())
                .couponId(payment.getCouponId())
                .subscriptionPlan(payment.getSubscriptionPlan())
                .billingCycle(payment.getBillingCycle())
                .billingPeriodStart(payment.getBillingPeriodStart())
                .billingPeriodEnd(payment.getBillingPeriodEnd())
                .amount(payment.getAmount())
                .discountAmount(payment.getDiscountAmount())
                .finalAmount(payment.getFinalAmount())
                .couponCode(payment.getCouponCode())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(newStatus)
                .pgProvider((String) pgResponse.get("provider"))
                .pgTransactionId((String) pgResponse.get("transactionId"))
                .webhookReceivedAt(LocalDateTime.now())
                .webhookData(pgResponse)
                .paidAt(newStatus == PaymentStatus.COMPLETED ? LocalDateTime.now() : null)
                .failedReason(newStatus == PaymentStatus.FAILED ? (String) pgResponse.get("failReason") : null)
                .build();

        paymentRepository.update(updatedPayment);

        log.info("결제 처리 완료: paymentId={}, status={}, txnId={}",
                payment.getId(), newStatus, updatedPayment.getPgTransactionId());

        // 10. 결제 성공 시
        if (newStatus == PaymentStatus.COMPLETED) {
            // 10.1 쿠폰 사용 처리
            if (couponId != null) {
                couponUsage = couponService.useCoupon(
                    couponId,
                    user.getId(),
                    payment.getId(),
                    amount
                );
                log.info("쿠폰 사용 완료: usageId={}, couponId={}, discountAmount={}",
                    couponUsage.getId(), couponId, couponUsage.getDiscountAmount());
            }

            // 10.2 구독 활성화
            subscriptionService.activateSubscriptionAfterPayment(
                    business.getId(),
                    payment.getSubscriptionPlan(),
                    billingCycle,
                    billingEnd.atStartOfDay()
            );
            log.info("구독 활성화 완료: businessId={}, plan={}, billingCycle={}",
                    business.getId(), payment.getSubscriptionPlan(), billingCycle);
        }

        return PaymentResponse.from(updatedPayment);
    }

    /**
     * 환불 처리 (일할 계산 적용)
     *
     * 환불 금액 계산:
     * - 잔여일수 = 청구 종료일 - 오늘
     * - 총 일수 = 청구 종료일 - 청구 시작일
     * - 환불 금액 = finalAmount × (잔여일수 / 총 일수)
     *
     * 참고: finalAmount는 실제 결제 금액 (할인 적용 후)
     * 프로덕션 PG 연동 시 VAT 포함 금액이 finalAmount로 저장되면 자동 반영됨
     */
    @Transactional
    public PaymentResponse refundPayment(Long paymentId, String reason) {
        log.info("환불 요청: paymentId={}, reason={}", paymentId, reason);

        // 1. Payment 조회
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.PAYMENT_NOT_FOUND));

        // 2. 환불 가능 여부 확인
        if (!payment.canRefund()) {
            throw new BusinessException(
                    ErrorCode.PAYMENT_CANNOT_REFUND,
                    "환불할 수 없는 결제입니다 (현재 상태: " + payment.getPaymentStatus() + ")"
            );
        }

        // 3. 일할 계산으로 환불 금액 산정
        int refundAmount = calculateProRataRefund(payment);

        // 4. PG 환불 호출 (Fake)
        Map<String, Object> refundResponse = fakePGService.requestRefund(
                payment.getPgTransactionId(),
                refundAmount,
                reason
        );

        // 5. Payment 업데이트
        Payment updatedPayment = Payment.builder()
                .id(payment.getId())
                .businessId(payment.getBusinessId())
                .couponId(payment.getCouponId())
                .subscriptionPlan(payment.getSubscriptionPlan())
                .billingCycle(payment.getBillingCycle())
                .billingPeriodStart(payment.getBillingPeriodStart())
                .billingPeriodEnd(payment.getBillingPeriodEnd())
                .amount(payment.getAmount())
                .discountAmount(payment.getDiscountAmount())
                .finalAmount(payment.getFinalAmount())
                .couponCode(payment.getCouponCode())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(PaymentStatus.REFUNDED)
                .pgProvider(payment.getPgProvider())
                .pgTransactionId(payment.getPgTransactionId())
                .webhookReceivedAt(payment.getWebhookReceivedAt())
                .webhookData(payment.getWebhookData())
                .paidAt(payment.getPaidAt())
                .failedReason(payment.getFailedReason())
                .refundedAt(LocalDateTime.now())
                .refundAmount(refundAmount)
                .build();

        paymentRepository.update(updatedPayment);

        // 6. 쿠폰 사용 취소
        couponService.cancelCouponUsage(paymentId);

        log.info("환불 완료: paymentId={}, 결제금액={}원, 환불금액={}원 (일할계산)",
                paymentId, payment.getFinalAmount(), refundAmount);

        return PaymentResponse.from(updatedPayment);
    }

    /**
     * 일할 계산 환불 금액 산정
     *
     * 계산 공식: finalAmount × (잔여일수 / 총일수)
     * - 결제 당일 환불 → 전액 환불
     * - 청구 기간 만료 후 → 환불 0원
     *
     * 예시 (월간 19,800원 기준, 30일 청구):
     * - 10일 사용 → 19,800 × 20/30 = 13,200원 환불
     * - 20일 사용 → 19,800 × 10/30 = 6,600원 환불
     * - 25일 사용 → 19,800 × 5/30 = 3,300원 환불
     */
    private int calculateProRataRefund(Payment payment) {
        LocalDate today = LocalDate.now();
        LocalDate start = payment.getBillingPeriodStart();
        LocalDate end = payment.getBillingPeriodEnd();

        // 결제 당일 환불 → 전액
        if (!today.isAfter(start)) {
            return payment.getFinalAmount();
        }

        // 청구 기간 만료 후 → 환불 불가
        if (!today.isBefore(end)) {
            return 0;
        }

        // 일할 계산
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(start, end);
        long remainingDays = java.time.temporal.ChronoUnit.DAYS.between(today, end);

        if (totalDays <= 0) {
            return payment.getFinalAmount();
        }

        // 반올림 처리 (원 단위)
        return (int) Math.round((double) payment.getFinalAmount() * remainingDays / totalDays);
    }

    /**
     * 결제 내역 조회 (단건)
     */
    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.PAYMENT_NOT_FOUND));
        return PaymentResponse.from(payment);
    }

    /**
     * 결제 내역 목록 조회 (검색)
     */
    public List<PaymentResponse> getPaymentList(PaymentSearchCondition condition) {
        return paymentRepository.findByCondition(condition)
                .stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 결제 내역 개수
     */
    public long countPayments(PaymentSearchCondition condition) {
        return paymentRepository.countByCondition(condition);
    }

    /**
     * PG 거래 ID로 결제 조회
     */
    public PaymentResponse getPaymentByPgTransactionId(String pgTransactionId) {
        Payment payment = paymentRepository.findByPgTransactionId(pgTransactionId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.PAYMENT_NOT_FOUND));
        return PaymentResponse.from(payment);
    }

    /**
     * 매장의 최근 결제 조회
     */
    public PaymentResponse getLatestPayment(Long businessId) {
        Payment payment = paymentRepository.findLatestByBusinessId(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.PAYMENT_NOT_FOUND));
        return PaymentResponse.from(payment);
    }
}
