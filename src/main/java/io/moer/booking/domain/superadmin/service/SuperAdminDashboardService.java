package io.moer.booking.domain.superadmin.service;

import io.moer.booking.domain.business.BusinessStatus;
import io.moer.booking.domain.business.BusinessType;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.reservation.repository.ReservationRepository;
import io.moer.booking.domain.superadmin.dto.BusinessRevenueRank;
import io.moer.booking.domain.superadmin.dto.BusinessTypeStats;
import io.moer.booking.domain.superadmin.dto.SystemStats;
import io.moer.booking.domain.user.UserRole;
import io.moer.booking.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 슈퍼 관리자 대시보드 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuperAdminDashboardService {

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    /**
     * 시스템 전체 통계
     */
    public SystemStats getSystemStats() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        return SystemStats.builder()
                // Business 통계
                .totalBusinesses(businessRepository.countByStatus(null))
                .activeBusinesses(businessRepository.countByStatus(BusinessStatus.ACTIVE))
                .inactiveBusinesses(businessRepository.countByStatus(BusinessStatus.INACTIVE))
                .suspendedBusinesses(businessRepository.countByStatus(BusinessStatus.SUSPENDED))

                // User 통계
                .totalUsers(userRepository.countAll())
                .superAdminCount(userRepository.countByRole(UserRole.SUPER_ADMIN))
                .adminCount(userRepository.countByRole(UserRole.ADMIN))
                .ownerCount(userRepository.countByRole(UserRole.OWNER))
                .staffCount(userRepository.countByRole(UserRole.STAFF))

                // Reservation 통계
                .totalReservationsToday(reservationRepository.countByDate(today))
                .totalRevenueToday(reservationRepository.sumTotalPriceByDate(today))
                .totalRevenueThisMonth(reservationRepository.sumTotalPriceByMonth(today))

                // 성장 지표
                .newBusinessesThisMonth(businessRepository.countCreatedInMonth(firstDayOfMonth))
                .newUsersThisMonth(userRepository.countCreatedInMonth(firstDayOfMonth))

                .build();
    }

    /**
     * 매장별 매출 랭킹
     */
    public List<BusinessRevenueRank> getTopBusinesses(LocalDate startDate, LocalDate endDate, int limit) {
        return businessRepository.getRevenueRankingByDateRange(startDate, endDate, limit);
    }

    /**
     * 업종별 통계
     */
    public List<BusinessTypeStats> getStatsByBusinessType() {
        List<BusinessTypeStats> result = new ArrayList<>();

        for (BusinessType type : BusinessType.values()) {
            long count = businessRepository.countByType(type);
            BigDecimal revenue = reservationRepository.sumRevenueByBusinessType(type);

            result.add(BusinessTypeStats.builder()
                    .businessType(type)
                    .count(count)
                    .totalRevenue(revenue != null ? revenue : BigDecimal.ZERO)
                    .build());
        }

        return result;
    }
}
