package io.moer.booking.domain.customer.repository;

import io.moer.booking.domain.customer.Customer;
import io.moer.booking.domain.customer.dto.CustomerSearchCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}