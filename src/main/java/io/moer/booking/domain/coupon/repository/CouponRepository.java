package io.moer.booking.domain.coupon.repository;

import io.moer.booking.domain.coupon.Coupon;
import io.moer.booking.domain.coupon.dto.CouponSearchCondition;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CouponRepository {
    void save(Coupon coupon);
    void update(Coupon coupon);
    void delete(Long id);
    Optional<Coupon> findById(Long id);
    Optional<Coupon> findByCode(String code);
    List<Coupon> findByCondition(CouponSearchCondition condition);
    long countByCondition(CouponSearchCondition condition);
    boolean existsByCode(String code);
    void incrementUsageCount(Long couponId);
    void decrementUsageCount(Long couponId);
}
