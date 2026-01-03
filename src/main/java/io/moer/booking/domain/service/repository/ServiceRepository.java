package io.moer.booking.domain.service.repository;

import io.moer.booking.domain.service.Service;
import io.moer.booking.domain.service.dto.ServiceSearchCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ServiceRepository {

    void save(Service service);

    Optional<Service> findById(Long id);
    List<Service> findByBusinessId(Long businessId);
    List<Service> findActiveByBusinessId(Long businessId);  // 추가
    List<Service> findByBusinessIdAndCategory(@Param("businessId") Long businessId,
                                              @Param("category") String category);
    List<Service> search(ServiceSearchCondition condition);  // 수정: findByCondition → search

    void update(Service service);
    void toggleActive(Long id);  // 추가

    void delete(Long id);

    boolean existsById(Long id);
    boolean existsByBusinessIdAndId(@Param("businessId") Long businessId, @Param("id") Long id);
    long countByBusinessId(Long businessId);
    long countByBusinessIdAndCategory(@Param("businessId") Long businessId, @Param("category") String category);
}