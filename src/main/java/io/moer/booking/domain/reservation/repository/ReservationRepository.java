package io.moer.booking.domain.reservation.repository;

import io.moer.booking.domain.business.BusinessType;
import io.moer.booking.domain.dashboard.dto.DailyCount;
import io.moer.booking.domain.dashboard.dto.RecentReservation;
import io.moer.booking.domain.reservation.Reservation;
import io.moer.booking.domain.reservation.ReservationStatus;
import io.moer.booking.domain.reservation.dto.ReservationSearchCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
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
     * 예약 출처(source) 업데이트
     */
    void updateSource(@Param("id") Long id, @Param("source") String source);

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

    // ========================================
    // 대시보드 통계 - 취소/노쇼
    // ========================================

    /**
     * 기간별 상태별 예약 수
     */
    int countByBusinessIdAndDateRangeAndStatus(
            Long businessId, LocalDate startDate, LocalDate endDate, ReservationStatus status);

    /**
     * 기간별 상태별 매출 손실액 (취소/노쇼)
     */
    Integer sumTotalPriceByBusinessIdAndDateRangeAndStatusIn(
            Long businessId, LocalDate startDate, LocalDate endDate, List<ReservationStatus> statuses);

    // ========================================
    // 대시보드 통계 - 액션 알림
    // ========================================

    /**
     * 1시간 이내 시작 예약 수
     */
    int countUpcomingReservations(
            @Param("businessId") Long businessId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    // ========================================
    // 대시보드 통계 - 직원 성과
    // ========================================

    /**
     * 직원별 예약 수 및 매출
     */
    List<io.moer.booking.domain.dashboard.dto.StaffPerformance> getStaffPerformance(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limit") int limit);

    // ========================================
    // 대시보드 통계 - 서비스 통계
    // ========================================

    /**
     * 인기 서비스 TOP N
     */
    List<io.moer.booking.domain.dashboard.dto.ServiceStats> getPopularServices(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limit") int limit);

    // ========================================
    // 대시보드 통계 - 평균 지표
    // ========================================

    /**
     * 평균 예약 금액
     */
    Integer getAverageReservationAmount(
            @Param("businessId") Long businessId,
            @Param("status") ReservationStatus status);

    /**
     * 평균 서비스 시간
     */
    Integer getAverageServiceDuration(
            @Param("businessId") Long businessId,
            @Param("status") ReservationStatus status);

    // ========================================
    // 대시보드 통계 - 매출 트렌드
    // ========================================

    /**
     * 특정 날짜의 매출
     */
    Integer getRevenueByDate(
            @Param("businessId") Long businessId,
            @Param("date") LocalDate date);

    /**
     * 기간별 매출
     */
    Integer getRevenueByDateRange(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 월별 매출 및 예약 수 (그래프용)
     */
    List<io.moer.booking.domain.dashboard.dto.MonthlyRevenue> getMonthlyRevenues(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 일별 매출 및 예약 수 (기간별 분석)
     */
    List<java.util.Map<String, Object>> getRevenueByDateRangeGroupByDate(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // ========================================
    // 대시보드 통계 - 시간대 분석
    // ========================================

    /**
     * 시간대별 예약 분포
     */
    List<io.moer.booking.domain.dashboard.dto.HourlyCount> getHourlyDistribution(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 요일별 예약 분포
     */
    List<io.moer.booking.domain.dashboard.dto.DayOfWeekCount> getWeekdayDistribution(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // ========================================
    // SuperAdmin 통계 쿼리
    // ========================================

    /**
     * 특정 날짜의 전체 예약 수
     */
    long countByDate(LocalDate date);

    /**
     * 특정 날짜의 전체 매출
     */
    BigDecimal sumTotalPriceByDate(LocalDate date);

    /**
     * 특정 월의 전체 매출
     */
    BigDecimal sumTotalPriceByMonth(LocalDate date);

    /**
     * 업종별 전체 매출
     */
    BigDecimal sumRevenueByBusinessType(BusinessType type);

    // ========================================
    // 로그인 고객(user_id) 예약 조회
    // ========================================

    /**
     * userId로 예약 목록 조회 (페이징, status 필터)
     */
    List<Reservation> findByUserId(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * userId로 예약 수
     */
    int countByUserId(
            @Param("userId") Long userId,
            @Param("status") String status);

    /**
     * userId + reservationNumber로 예약 조회
     */
    Optional<Reservation> findByUserIdAndReservationNumber(
            @Param("userId") Long userId,
            @Param("reservationNumber") String reservationNumber);

    /**
     * 예약의 user_id 업데이트
     */
    void updateUserId(
            @Param("reservationId") Long reservationId,
            @Param("userId") Long userId);

    // ========================================
    // 고객 예약 이력 조회
    // ========================================

    /**
     * 고객별 예약 목록 (페이징, status 선택적 필터)
     */
    List<Reservation> findByBusinessIdAndCustomerId(
            @Param("businessId") Long businessId,
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 고객별 예약 수 (status 선택적 필터)
     */
    int countByBusinessIdAndCustomerId(
            @Param("businessId") Long businessId,
            @Param("customerId") Long customerId,
            @Param("status") String status);

    /**
     * 가장 많이 이용한 서비스명 (JSONB 집계, COMPLETED 상태만)
     */
    String findFavoriteServiceByCustomerId(
            @Param("businessId") Long businessId,
            @Param("customerId") Long customerId);

    /**
     * 가장 많이 만난 직원 ID (COMPLETED 상태만)
     */
    Long findFavoriteStaffIdByCustomerId(
            @Param("businessId") Long businessId,
            @Param("customerId") Long customerId);
}