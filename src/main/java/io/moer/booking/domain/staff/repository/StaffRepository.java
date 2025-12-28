package io.moer.booking.domain.staff.repository;

import io.moer.booking.domain.staff.Staff;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface StaffRepository {

    // 생성
    void save(Staff staff);

    // 조회
    Optional<Staff> findById(Long id);
    List<Staff> findByBusinessId(Long businessId);
    List<Staff> findByBusinessIdAndActive(@Param("businessId") Long businessId,
                                          @Param("isActive") Boolean isActive);

    // 수정
    void update(Staff staff);

    // 삭제
    void delete(Long id);

    // 검증
    boolean existsById(Long id);
    boolean existsByBusinessIdAndId(@Param("businessId") Long businessId,
                                    @Param("id") Long id);
    long countByBusinessId(Long businessId);
}