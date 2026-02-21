package io.moer.booking.domain.inquiry.repository;

import io.moer.booking.domain.inquiry.Inquiry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 문의 Repository
 */
@Mapper
public interface InquiryRepository {

    /**
     * 문의 저장
     */
    void save(Inquiry inquiry);

    /**
     * 특정 IP에서 특정 시각 이후 문의 건수 조회 (Rate Limiting용)
     */
    int countByIpAddressAndCreatedAtAfter(
            @Param("ipAddress") String ipAddress,
            @Param("after") LocalDateTime after
    );
}
