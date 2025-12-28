package io.moer.booking.domain.holiday.repository;

import io.moer.booking.domain.holiday.SpecialHoliday;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Mapper
public interface SpecialHolidayRepository {

    // 생성
    void save(SpecialHoliday holiday);

    // 조회
    Optional<SpecialHoliday> findById(Long id);
    List<SpecialHoliday> findByBusinessId(Long businessId);
    List<SpecialHoliday> findByBusinessIdAndDateRange(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    Optional<SpecialHoliday> findByBusinessIdAndDate(
            @Param("businessId") Long businessId,
            @Param("holidayDate") LocalDate holidayDate
    );

    // 삭제
    void delete(Long id);
    void deleteByBusinessIdAndDate(
            @Param("businessId") Long businessId,
            @Param("holidayDate") LocalDate holidayDate
    );

    // 검증
    boolean existsById(Long id);
    boolean existsByBusinessIdAndDate(
            @Param("businessId") Long businessId,
            @Param("holidayDate") LocalDate holidayDate
    );
    boolean existsByBusinessIdAndId(
            @Param("businessId") Long businessId,
            @Param("id") Long id
    );
}