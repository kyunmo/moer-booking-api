package io.moer.booking.domain.staff.position.repository;

import io.moer.booking.domain.staff.position.StaffPosition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface StaffPositionRepository {

    void save(StaffPosition position);

    Optional<StaffPosition> findById(Long id);

    List<StaffPosition> findByBusinessId(Long businessId);

    void update(StaffPosition position);

    void updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);

    void delete(Long id);

    boolean existsById(Long id);

    boolean existsByBusinessIdAndId(@Param("businessId") Long businessId, @Param("id") Long id);

    boolean existsByBusinessIdAndName(@Param("businessId") Long businessId, @Param("name") String name);

    boolean existsByBusinessIdAndNameAndIdNot(@Param("businessId") Long businessId,
                                              @Param("name") String name,
                                              @Param("id") Long id);

    int countStaffsByPositionId(Long positionId);

    int getMaxSortOrderByBusinessId(Long businessId);
}
