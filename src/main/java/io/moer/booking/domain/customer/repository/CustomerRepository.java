package io.moer.booking.domain.customer.repository;

import io.moer.booking.domain.customer.Customer;
import io.moer.booking.domain.customer.dto.CustomerSearchCondition;
import io.moer.booking.domain.dashboard.dto.RecentCustomer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface CustomerRepository {

    // 생성
    void save(Customer customer);

    // 조회
    Optional<Customer> findById(Long id);
    List<Customer> findByBusinessId(Long businessId);
    Optional<Customer> findByBusinessIdAndPhone(@Param("businessId") Long businessId,
                                                @Param("phone") String phone);
    List<Customer> findByCondition(CustomerSearchCondition condition);

    // 통계
    List<Customer> findVipCustomers(Long businessId);  // VIP 고객 (10회 이상)
    List<Customer> findNewCustomers(Long businessId);  // 신규 고객 (1회)
    List<Customer> findRegularCustomers(Long businessId);  // 단골 고객 (3회 이상)

    // 수정
    void update(Customer customer);
    void updateVisitStats(@Param("id") Long id,
                          @Param("visitCount") Integer visitCount,
                          @Param("totalSpent") Integer totalSpent,
                          @Param("lastVisitDate") java.time.LocalDate lastVisitDate);

    // 삭제
    void delete(Long id);

    // 검증
    boolean existsById(Long id);
    boolean existsByBusinessIdAndPhone(@Param("businessId") Long businessId,
                                       @Param("phone") String phone);
    boolean existsByBusinessIdAndId(@Param("businessId") Long businessId,
                                    @Param("id") Long id);
    long countByBusinessId(Long businessId);


    /**
     * 기간별 신규 고객 수
     */
    int countByBusinessIdAndCreatedAtBetween(
            Long businessId, LocalDateTime startDateTime, LocalDateTime endDateTime);

    /**
     * 최근 신규 고객
     */
    List<RecentCustomer> findRecentByBusinessId(Long businessId, int limit);

    // ========================================
    // 대시보드 통계 - 액션 알림
    // ========================================

    /**
     * 오늘 생일인 고객 수
     */
    int countBirthdayCustomers(@Param("businessId") Long businessId,
                                @Param("month") int month,
                                @Param("day") int day);

    /**
     * 재방문 유도 대상 고객 수 (N개월 이상 미방문)
     */
    int countInactiveCustomers(@Param("businessId") Long businessId,
                                @Param("monthsAgo") int monthsAgo);

    // ========================================
    // 대시보드 통계 - 고객 세그먼트
    // ========================================

    /**
     * 단골 고객 수 (minVisits ~ maxVisits 회 방문)
     */
    int countByBusinessIdAndVisitCountBetween(
            @Param("businessId") Long businessId,
            @Param("minVisits") int minVisits,
            @Param("maxVisits") int maxVisits);

    /**
     * 이탈 고객 수 (N개월 이상 미방문)
     */
    int countInactiveCustomersByLastVisit(
            @Param("businessId") Long businessId,
            @Param("monthsAgo") int monthsAgo);

    /**
     * 재방문 고객 수 (2회 이상)
     */
    int countReturningCustomers(@Param("businessId") Long businessId);

    // ========================================
    // 대시보드 통계 - 평균 지표
    // ========================================

    /**
     * 평균 방문 횟수
     */
    Double getAverageVisitCount(@Param("businessId") Long businessId);

    /**
     * 평균 고객 생애 가치 (LTV)
     */
    Integer getAverageCustomerLifetimeValue(@Param("businessId") Long businessId);
}