package io.moer.booking.domain.staff.repository;

import io.moer.booking.domain.staff.Portfolio;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

/**
 * 포트폴리오 Repository
 */
@Mapper
public interface PortfolioRepository {

    // ========================================
    // CUD
    // ========================================

    /**
     * 포트폴리오 생성
     */
    void save(Portfolio portfolio);

    /**
     * 포트폴리오 공개/비공개 전환
     */
    void toggleVisibility(Long id);

    /**
     * 포트폴리오 삭제
     */
    void delete(Long id);

    // ========================================
    // 조회
    // ========================================

    /**
     * ID로 조회
     */
    Optional<Portfolio> findById(Long id);

    /**
     * Staff의 전체 포트폴리오 조회
     */
    List<Portfolio> findByStaffId(Long staffId);

    /**
     * Staff의 공개 포트폴리오 조회
     */
    List<Portfolio> findVisibleByStaffId(Long staffId);

    /**
     * Business의 전체 포트폴리오 조회
     */
    List<Portfolio> findByBusinessId(Long businessId);

    // ========================================
    // 검증
    // ========================================

    /**
     * ID 존재 확인
     */
    boolean existsById(Long id);
}