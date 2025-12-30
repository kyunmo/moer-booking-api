package io.moer.booking.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.user.User;
import io.moer.booking.domain.user.UserRole;
import io.moer.booking.domain.user.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private UserRole role;
    private String roleDescription;
    private UserStatus status;
    private String statusDescription;
    private Long staffId;
    private Long businessId;
    private Boolean emailVerified;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .role(user.getRole())
                .roleDescription(user.getRole().getDescription())
                .status(user.getStatus())
                .statusDescription(user.getStatus().getDescription())
                .staffId(user.getStaffId())
                .businessId(user.getBusinessId())
                .emailVerified(user.getEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}