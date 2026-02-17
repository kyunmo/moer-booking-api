package io.moer.booking.domain.statistics.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 통계 분석 Repository
 * - 매출, 예약, 고객, 스태프, 서비스 통계 쿼리
 * - 모든 메서드는 Map<String, Object>를 반환하며 Service에서 DTO로 변환
 */
@Mapper
public interface StatisticsRepository {

    // ========================================
    // Revenue Statistics (매출 통계)
    // ========================================

    /**
     * 매출 요약 (총 매출, 완료 건수, 평균 거래액, 완료율)
     */
    Map<String, Object> getRevenueSummary(@Param("businessId") Long businessId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    /**
     * 매출 트렌드 (일별/주별/월별 그룹)
     */
    List<Map<String, Object>> getRevenueTrend(@Param("businessId") Long businessId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("groupBy") String groupBy);

    /**
     * 서비스별 매출 비중 (JSONB 서비스 배열 집계)
     */
    List<Map<String, Object>> getRevenueByService(@Param("businessId") Long businessId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    /**
     * 결제 수단별 매출 비중
     */
    List<Map<String, Object>> getRevenueByPaymentMethod(@Param("businessId") Long businessId,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);

    // ========================================
    // Reservation Statistics (예약 통계)
    // ========================================

    /**
     * 예약 요약 (상태별 건수, 비율, 손실 매출)
     */
    Map<String, Object> getReservationSummary(@Param("businessId") Long businessId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    /**
     * 예약 트렌드 (일별/주별/월별 그룹)
     */
    List<Map<String, Object>> getReservationTrend(@Param("businessId") Long businessId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate,
                                                    @Param("groupBy") String groupBy);

    /**
     * 시간대 x 요일 히트맵 (ISODOW 1=월~7=일)
     */
    List<Map<String, Object>> getHourlyHeatmap(@Param("businessId") Long businessId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    /**
     * 상태별 예약 분포
     */
    List<Map<String, Object>> getStatusDistribution(@Param("businessId") Long businessId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    /**
     * 요일별 예약 분포 (평균 + 총합)
     */
    List<Map<String, Object>> getDailyDistribution(@Param("businessId") Long businessId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    // ========================================
    // Customer Statistics (고객 통계)
    // ========================================

    /**
     * 고객 요약 (총 고객 수, 신규, 재방문율, 평균 방문, 평균 LTV, 이탈율)
     */
    Map<String, Object> getCustomerSummary(@Param("businessId") Long businessId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * 고객 트렌드 (월별 신규/재방문/활성/이탈)
     */
    List<Map<String, Object>> getCustomerTrend(@Param("businessId") Long businessId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    /**
     * 고객 세그먼트 (VIP/REGULAR/NEW/INACTIVE)
     */
    List<Map<String, Object>> getCustomerSegments(@Param("businessId") Long businessId);

    /**
     * 재방문율 추이 (월별)
     */
    List<Map<String, Object>> getReturningRateTrend(@Param("businessId") Long businessId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    /**
     * LTV 분포 (지출 구간별 고객 수)
     */
    List<Map<String, Object>> getLtvDistribution(@Param("businessId") Long businessId);

    // ========================================
    // Staff Statistics (스태프 통계)
    // ========================================

    /**
     * 스태프별 성과 (예약 수, 매출, 완료율 등)
     */
    List<Map<String, Object>> getStaffPerformances(@Param("businessId") Long businessId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate,
                                                     @Param("staffId") Long staffId);

    /**
     * 스태프별 매출 트렌드 (월별)
     */
    List<Map<String, Object>> getStaffRevenueTrend(@Param("businessId") Long businessId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate,
                                                     @Param("staffId") Long staffId);

    /**
     * 스태프 레이더 차트 원시 데이터 (예약량, 매출, 완료율, 평점, 효율)
     */
    List<Map<String, Object>> getStaffRadarData(@Param("businessId") Long businessId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    // ========================================
    // Service Statistics (서비스 통계)
    // ========================================

    /**
     * 서비스 요약 (총 이용 건수, 고유 서비스 수, 평균 가격, 카테고리 수, 최인기/최수익)
     */
    Map<String, Object> getServiceSummary(@Param("businessId") Long businessId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    /**
     * 서비스 랭킹 (예약 수, 매출 등)
     */
    List<Map<String, Object>> getServiceRankings(@Param("businessId") Long businessId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate,
                                                   @Param("categoryId") Long categoryId);

    /**
     * 카테고리별 분포 (예약 수, 매출)
     */
    List<Map<String, Object>> getCategoryDistribution(@Param("businessId") Long businessId,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);

    /**
     * 서비스 트렌드 (상위 5개 서비스의 월별 추이)
     */
    List<Map<String, Object>> getServiceTrend(@Param("businessId") Long businessId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);
}
