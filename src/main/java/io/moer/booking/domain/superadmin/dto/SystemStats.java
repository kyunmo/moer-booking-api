package io.moer.booking.domain.superadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 시스템 전체 통계
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemStats {
    // Business 통계
    private Long totalBusinesses;
    private Long activeBusinesses;
    private Long inactiveBusinesses;
    private Long suspendedBusinesses;

    // User 통계
    private Long totalUsers;
    private Long superAdminCount;
    private Long adminCount;
    private Long ownerCount;
    private Long staffCount;

    // Reservation 통계
    private Long totalReservationsToday;
    private BigDecimal totalRevenueToday;
    private BigDecimal totalRevenueThisMonth;

    // 성장 지표
    private Long newBusinessesThisMonth;
    private Long newUsersThisMonth;
}
