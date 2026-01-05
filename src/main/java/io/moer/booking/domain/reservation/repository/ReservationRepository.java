package io.moer.booking.domain.reservation.repository;

import io.moer.booking.domain.dashboard.dto.DailyCount;
import io.moer.booking.domain.dashboard.dto.RecentReservation;
import io.moer.booking.domain.reservation.Reservation;
import io.moer.booking.domain.reservation.ReservationStatus;
import io.moer.booking.domain.reservation.dto.ReservationSearchCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 예약 Repository
 */
@Mapper
public interface ReservationRepository {

    // ========================================
    // CUD
    // ========================================

    /**
     * 예약 생성
     */
    void save(Reservation reservation);

    /**
     * 예약 수정
     */
    void update(Reservation reservation);

    /**
     * 예약 상태만 변경
     */
    void updateStatus(@Param("id") Long id, @Param("status") ReservationStatus status);

    /**
     * 예약 취소 정보 업데이트
     */
    void updateCancellation(Reservation reservation);

    /**
     * 예약 삭제
     */
    void delete(Long id);

    // ========================================
    // 조회 - 단건
    // ========================================

    /**
     * ID로 조회
     */
    Optional<Reservation> findById(Long id);

    /**
     * 예약번호로 조회
     */
    Optional<Reservation> findByReservationNumber(String reservationNumber);

    // ========================================
    // 조회 - 목록
    // ========================================

    /**
     * Business의 전체 예약 조회
     */
    List<Reservation> findByBusinessId(Long businessId);

    /**
     * Customer의 예약 조회
     */
    List<Reservation> findByCustomerId(Long customerId);

    /**
     * Staff의 예약 조회
     */
    List<Reservation> findByStaffId(Long staffId);

    /**
     * 특정 날짜의 예약 조회
     */
    List<Reservation> findByBusinessIdAndDate(
            @Param("businessId") Long businessId,
            @Param("date") LocalDate date
    );

    /**
     * 기간별 예약 조회
     */
    List<Reservation> findByBusinessIdAndDateRange(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 상태별 예약 조회
     */
    List<Reservation> findByBusinessIdAndStatus(
            @Param("businessId") Long businessId,
            @Param("status") ReservationStatus status
    );

    /**
     * 조건별 예약 검색
     */
    List<Reservation> search(ReservationSearchCondition condition);

    // ========================================
    // 검증
    // ========================================

    /**
     * 예약 시간 충돌 체크
     */
    List<Reservation> findConflictingReservations(
            @Param("staffId") Long staffId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /**
     * ID 존재 확인
     */
    boolean existsById(Long id);

    /**
     * Business의 예약 존재 확인
     */
    boolean existsByBusinessIdAndId(
            @Param("businessId") Long businessId,
            @Param("id") Long id
    );

    /**
     * 예약번호 존재 확인
     */
    boolean existsByReservationNumber(String reservationNumber);


    /**
     * 날짜별 예약 수
     */
    int countByBusinessIdAndDate(Long businessId, LocalDate date);

    int countByBusinessIdAndDateAndStatus(Long businessId, LocalDate date, ReservationStatus status);

    /**
     * 기간별 예약 수
     */
    int countByBusinessIdAndDateRange(Long businessId, LocalDate startDate, LocalDate endDate);

    /**
     * 기간별 매출 합계 (완료 상태만)
     */
    Integer sumTotalPriceByBusinessIdAndDateRangeAndStatus(
            Long businessId, LocalDate startDate, LocalDate endDate, ReservationStatus status);

    /**
     * 오늘 예상 매출 (대기+확정)
     */
    Integer sumTotalPriceByBusinessIdAndDateAndStatusIn(
            Long businessId, LocalDate date, List<ReservationStatus> statuses);

    /**
     * 일별 예약 건수 (그룹화)
     */
    List<DailyCount> countByBusinessIdAndDateRangeGroupByDate(
            Long businessId, LocalDate startDate, LocalDate endDate);

    /**
     * 최근 예약 목록
     */
    List<RecentReservation> findRecentByBusinessIdAndDate(Long businessId, LocalDate date, int limit);
}