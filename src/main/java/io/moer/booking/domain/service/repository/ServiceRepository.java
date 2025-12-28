package io.moer.booking.domain.service.repository;

import io.moer.booking.domain.service.Service;
import io.moer.booking.domain.service.dto.ServiceSearchCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ServiceRepository {

    // 생성
    void save(Service service);

    // 조회
    Optional<Service> findById(Long id);
    List<Service> findByBusinessId(Long businessId);
    List<Service> findByBusinessIdAndActive(@Param("businessId") Long businessId,
                                            @Param("isActive") Boolean isActive);
    List<Service> findByCondition(ServiceSearchCondition condition);
    List<Service> findByBusinessIdAndCategory(@Param("businessId") Long businessId,
                                              @Param("category") String category);

    // 수정
    void update(Service service);

    // 삭제
    void delete(Long id);

    // 검증
    boolean existsById(Long id);
    boolean existsByBusinessIdAndId(@Param("businessId") Long businessId,
                                    @Param("id") Long id);
    long countByBusinessId(Long businessId);
    long countByBusinessIdAndCategory(@Param("businessId") Long businessId,
                                      @Param("category") String category);
}