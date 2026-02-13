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
    List<Service> findActiveByBusinessId(Long businessId);
    List<Service> findByBusinessIdAndCategoryId(@Param("businessId") Long businessId,
                                                @Param("categoryId") Long categoryId);
    List<Service> search(ServiceSearchCondition condition);

    void update(Service service);
    void updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);
    void toggleActive(Long id);

    void delete(Long id);

    boolean existsById(Long id);
    boolean existsByBusinessIdAndId(@Param("businessId") Long businessId, @Param("id") Long id);
    long countByBusinessId(Long businessId);
    long countByBusinessIdAndCategoryId(@Param("businessId") Long businessId, @Param("categoryId") Long categoryId);
}
