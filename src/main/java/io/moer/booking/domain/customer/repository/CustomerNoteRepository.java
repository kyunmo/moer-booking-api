package io.moer.booking.domain.customer.repository;

import io.moer.booking.domain.customer.CustomerNote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CustomerNoteRepository {

    void save(CustomerNote note);

    Optional<CustomerNote> findById(Long id);

    List<CustomerNote> findByCustomerIdAndBusinessId(
            @Param("customerId") Long customerId,
            @Param("businessId") Long businessId);

    void update(CustomerNote note);

    void deleteById(Long id);

    /**
     * 고객 병합 시 메모의 customer_id 일괄 변경
     */
    int updateCustomerId(
            @Param("fromCustomerId") Long fromCustomerId,
            @Param("toCustomerId") Long toCustomerId);

    /**
     * 특정 고객의 메모 수 조회
     */
    int countByCustomerId(@Param("customerId") Long customerId);
}
