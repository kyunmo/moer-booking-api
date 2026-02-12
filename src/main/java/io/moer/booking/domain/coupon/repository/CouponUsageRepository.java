package io.moer.booking.domain.coupon.repository;

import io.moer.booking.domain.coupon.CouponUsage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CouponUsageRepository {
    void save(CouponUsage usage);
    void update(CouponUsage usage);
    Optional<CouponUsage> findById(Long id);
    List<CouponUsage> findByCouponId(Long couponId);
    List<CouponUsage> findByUserId(Long userId);
    Optional<CouponUsage> findByPaymentId(Long paymentId);
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);
}
