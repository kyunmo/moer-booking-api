package io.moer.booking.domain.staff.repository;

import io.moer.booking.domain.staff.Staff;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 직원 Repository
 */
@Mapper
public interface StaffRepository {

    // ========================================
    // CUD
    // ========================================

    /**
     * 직원 생성
     */
    void save(Staff staff);

    /**
     * 직원 수정
     */
    void update(Staff staff);

    /**
     * 직원 활성/비활성 전환
     */
    void toggleActive(Long id);

    /**
     * 직원 삭제
     */
    void delete(Long id);

    // ========================================
    // 조회
    // ========================================

    /**
     * ID로 조회
     */
    Optional<Staff> findById(Long id);

    /**
     * Business의 전체 직원 조회
     */
    List<Staff> findByBusinessId(Long businessId);

    /**
     * Business의 활성 직원 조회
     */
    List<Staff> findActiveByBusinessId(Long businessId);

    // ========================================
    // 검증
    // ========================================

    /**
     * ID 존재 확인
     */
    boolean existsById(Long id);

    /**
     * Business의 직원 존재 확인
     */
    boolean existsByBusinessIdAndId(
            @Param("businessId") Long businessId,
            @Param("id") Long id
    );

    /**
     * Business의 직원 수 조회
     */
    long countByBusinessId(Long businessId);
}