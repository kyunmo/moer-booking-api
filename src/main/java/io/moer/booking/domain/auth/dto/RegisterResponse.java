package io.moer.booking.domain.auth.dto;

import io.moer.booking.domain.business.dto.BusinessResponse;
import io.moer.booking.domain.user.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;

    private UserResponse user;
    private BusinessResponse business;

    public static RegisterResponse of(
            String accessToken,
            String refreshToken,
            Long expiresIn,
            UserResponse user,
            BusinessResponse business) {
        return RegisterResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(user)
                .business(business)
                .build();
    }
}