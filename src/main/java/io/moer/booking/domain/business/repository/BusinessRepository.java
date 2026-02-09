package io.moer.booking.domain.business.repository;

import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.BusinessStatus;
import io.moer.booking.domain.business.BusinessType;
import io.moer.booking.domain.business.dto.BusinessSearchCondition;
import io.moer.booking.domain.superadmin.dto.BusinessRevenueRank;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Mapper
public interface BusinessRepository {

    // 생성
    void save(Business business);

    // 조회
    Optional<Business> findById(Long id);
    List<Business> findAll(BusinessSearchCondition condition);
    long countAll(BusinessSearchCondition condition);
    List<Business> findByOwnerId(Long ownerId);

    // 수정
    void update(Business business);

    // 삭제
    void delete(Long id);

    // 검증
    boolean existsById(Long id);
    long countByOwnerId(Long ownerId);

    // SuperAdmin 통계 쿼리
    long countByStatus(BusinessStatus status);
    long countByType(BusinessType type);
    long countCreatedInMonth(LocalDate date);
    List<BusinessRevenueRank> getRevenueRankingByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limit") int limit
    );
}