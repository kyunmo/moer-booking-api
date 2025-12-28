package io.moer.booking.domain.business.repository;

import io.moer.booking.domain.business.BusinessSettings;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface BusinessSettingsRepository {

    // 생성
    void save(BusinessSettings settings);

    // 조회
    Optional<BusinessSettings> findByBusinessId(Long businessId);

    // 수정
    void update(BusinessSettings settings);

    // 삭제
    void deleteByBusinessId(Long businessId);
}