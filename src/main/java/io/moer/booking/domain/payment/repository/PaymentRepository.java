package io.moer.booking.domain.payment.repository;

import io.moer.booking.domain.payment.Payment;
import io.moer.booking.domain.payment.dto.PaymentSearchCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 결제 Repository
 */
@Mapper
public interface PaymentRepository {

    /**
     * 결제 저장
     */
    void save(Payment payment);

    /**
     * 결제 수정
     */
    void update(Payment payment);

    /**
     * ID로 결제 조회
     */
    Optional<Payment> findById(Long id);

    /**
     * 조건에 맞는 결제 목록 조회
     */
    List<Payment> findByCondition(PaymentSearchCondition condition);

    /**
     * 조건에 맞는 결제 개수
     */
    long countByCondition(PaymentSearchCondition condition);

    /**
     * PG 거래 ID로 결제 조회
     */
    Optional<Payment> findByPgTransactionId(@Param("pgTransactionId") String pgTransactionId);

    /**
     * 매장의 최근 결제 조회
     */
    Optional<Payment> findLatestByBusinessId(@Param("businessId") Long businessId);
}
