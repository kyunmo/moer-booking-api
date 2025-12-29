package io.moer.booking.domain.customer.repository;

import io.moer.booking.domain.customer.CustomerHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Mapper
public interface CustomerHistoryRepository {

    // 생성
    void save(CustomerHistory history);

    // 조회
    Optional<CustomerHistory> findById(Long id);
    List<CustomerHistory> findByCustomerId(Long customerId);
    List<CustomerHistory> findByBusinessId(Long businessId);
    List<CustomerHistory> findByReservationId(Long reservationId);
    List<CustomerHistory> findByCustomerIdAndDateRange(
            @Param("customerId") Long customerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // 최근 방문 이력 조회
    Optional<CustomerHistory> findLatestByCustomerId(Long customerId);

    // 수정
    void update(CustomerHistory history);

    // 삭제
    void delete(Long id);

    // 검증
    boolean existsById(Long id);
    boolean existsByBusinessIdAndId(@Param("businessId") Long businessId, @Param("id") Long id);
}