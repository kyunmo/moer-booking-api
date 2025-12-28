package io.moer.booking.domain.business.repository;

import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.dto.BusinessSearchCondition;
import org.apache.ibatis.annotations.Mapper;

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
}