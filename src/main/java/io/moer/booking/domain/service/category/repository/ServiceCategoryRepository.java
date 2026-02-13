package io.moer.booking.domain.service.category.repository;

import io.moer.booking.domain.service.category.ServiceCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ServiceCategoryRepository {

    void save(ServiceCategory category);

    Optional<ServiceCategory> findById(Long id);

    List<ServiceCategory> findByBusinessId(Long businessId);

    void update(ServiceCategory category);

    void updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);

    void delete(Long id);

    boolean existsById(Long id);

    boolean existsByBusinessIdAndId(@Param("businessId") Long businessId, @Param("id") Long id);

    boolean existsByBusinessIdAndName(@Param("businessId") Long businessId, @Param("name") String name);

    boolean existsByBusinessIdAndNameAndIdNot(@Param("businessId") Long businessId,
                                              @Param("name") String name,
                                              @Param("id") Long id);

    int countServicesByCategoryId(Long categoryId);

    int getMaxSortOrderByBusinessId(Long businessId);
}
