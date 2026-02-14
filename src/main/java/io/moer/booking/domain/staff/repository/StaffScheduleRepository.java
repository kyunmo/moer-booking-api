package io.moer.booking.domain.staff.repository;

import io.moer.booking.domain.staff.StaffSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 직원 근무 스케줄 Repository
 */
@Mapper
public interface StaffScheduleRepository {

    /**
     * 스케줄 저장
     */
    void save(StaffSchedule schedule);

    /**
     * 직원의 전체 스케줄 삭제
     */
    void deleteByStaffId(@Param("staffId") Long staffId);

    /**
     * 직원의 전체 스케줄 조회 (요일순)
     */
    List<StaffSchedule> findByStaffId(@Param("staffId") Long staffId);

    /**
     * 직원의 특정 요일 스케줄 조회
     */
    Optional<StaffSchedule> findByStaffIdAndDayOfWeek(
            @Param("staffId") Long staffId,
            @Param("dayOfWeek") Integer dayOfWeek
    );
}
