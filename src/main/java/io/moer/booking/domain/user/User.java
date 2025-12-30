package io.moer.booking.domain.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private Long id;
    private String email;
    private String password;
    private String name;
    private String phone;
    private UserRole role;
    private UserStatus status;
    private Long staffId;        // STAFF 역할인 경우
    private Long businessId;     // OWNER/STAFF 소속 매장
    private Boolean emailVerified;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 권한 체크 헬퍼 메서드
    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }

    public boolean isOwner() {
        return this.role == UserRole.OWNER;
    }

    public boolean isStaff() {
        return this.role == UserRole.STAFF;
    }

    public boolean canAccessBusiness(Long businessId) {
        if (isAdmin()) {
            return true;
        }
        return this.businessId != null && this.businessId.equals(businessId);
    }

    public boolean canAccessStaff(Long staffId) {
        if (isAdmin()) {
            return true;
        }
        if (isOwner()) {
            return true; // 같은 매장이면 OK (서비스 레이어에서 추가 검증)
        }
        if (isStaff()) {
            return this.staffId != null && this.staffId.equals(staffId);
        }
        return false;
    }
}