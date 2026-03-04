package io.moer.booking.domain.service.repository;

import io.moer.booking.domain.service.ServiceImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 서비스 이미지 Repository
 */
@Mapper
public interface ServiceImageRepository {

    /**
     * 서비스 이미지 저장
     */
    void save(ServiceImage image);

    /**
     * ID로 조회
     */
    Optional<ServiceImage> findById(Long id);

    /**
     * 서비스 ID로 이미지 목록 조회
     */
    List<ServiceImage> findByServiceId(Long serviceId);

    /**
     * 서비스 ID로 이미지 수 조회
     */
    int countByServiceId(Long serviceId);

    /**
     * 정렬 순서 변경
     */
    void updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);

    /**
     * ID로 삭제
     */
    void deleteById(Long id);

    /**
     * 서비스 ID로 전체 이미지 삭제
     */
    void deleteByServiceId(Long serviceId);
}
