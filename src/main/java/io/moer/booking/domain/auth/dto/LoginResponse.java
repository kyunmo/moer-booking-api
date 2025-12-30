package io.moer.booking.domain.auth.dto;

import io.moer.booking.domain.user.User;
import io.moer.booking.domain.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;  // 초 단위

    // 사용자 정보
    private Long userId;
    private String email;
    private String name;
    private UserRole role;
    private Long staffId;
    private Long businessId;

    public static LoginResponse of(
            String accessToken,
            String refreshToken,
            Long expiresIn,
            User user) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .staffId(user.getStaffId())
                .businessId(user.getBusinessId())
                .build();
    }
}