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
    List<SpecialHoliday> findByBusinessIdAndDateRange(Long businessId, LocalDate startDate, LocalDate endDate);
    Optional<SpecialHoliday> findByBusinessIdAndDate(Long businessId, LocalDate date);

    // 연도별 조회
    List<SpecialHoliday> findByBusinessIdAndYear(@Param("businessId") Long businessId, @Param("year") int year);

    // 존재 여부
    boolean existsByBusinessIdAndId(Long businessId, Long id);
    boolean existsByBusinessIdAndDate(Long businessId, LocalDate date);

    // 삭제
    void delete(Long id);
    void deleteByBusinessIdAndDate(Long businessId, LocalDate date);
}