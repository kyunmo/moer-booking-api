package io.moer.booking.common.security;

import io.moer.booking.domain.user.User;
import io.moer.booking.domain.user.UserRole;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final User user;

    public Long getUserId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public UserRole getRole() {
        return user.getRole();
    }

    public Long getStaffId() {
        return user.getStaffId();
    }

    public Long getBusinessId() {
        return user.getBusinessId();
    }

    public boolean isSuperAdmin() {
        return user.isSuperAdmin();
    }

    public boolean isAdmin() {
        return user.isAdmin();
    }

    public boolean isOwner() {
        return user.isOwner();
    }

    public boolean isStaff() {
        return user.isStaff();
    }

    public boolean isCustomer() {
        return user.isCustomer();
    }

    public boolean canAccessBusiness(Long businessId) {
        return user.canAccessBusiness(businessId);
    }

    public boolean canAccessStaff(Long staffId) {
        return user.canAccessStaff(staffId);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus().name().equals("ACTIVE");
    }
}