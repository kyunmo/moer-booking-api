package io.moer.booking.domain.staff.repository;

import io.moer.booking.domain.staff.Portfolio;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface PortfolioRepository {

    // 생성
    void save(Portfolio portfolio);

    // 조회
    Optional<Portfolio> findById(Long id);
    List<Portfolio> findByStaffId(Long staffId);
    List<Portfolio> findByBusinessId(Long businessId);
    List<Portfolio> findByStaffIdAndVisible(@Param("staffId") Long staffId,
                                            @Param("isVisible") Boolean isVisible);

    // 수정
    void update(Portfolio portfolio);

    // 삭제
    void delete(Long id);
    void deleteByStaffId(Long staffId);
}