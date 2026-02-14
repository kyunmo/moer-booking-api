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
    private String profileImageUrl;
    private UserRole role;  // Enum 그대로
    private String roleDescription;
    private UserStatus status;  // Enum 그대로
    private String statusDescription;
    private Long staffId;
    private Long businessId;
    private Boolean emailVerified;  // boolean

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
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .roleDescription(user.getRole().getDescription())  // Enum 사용
                .status(user.getStatus())
                .statusDescription(user.getStatus().getDescription())  // Enum 사용
                .staffId(user.getStaffId())
                .businessId(user.getBusinessId())
                .emailVerified("Y".equals(user.getEmailVerified()))
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}