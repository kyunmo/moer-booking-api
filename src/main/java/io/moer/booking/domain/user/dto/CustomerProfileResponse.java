package io.moer.booking.domain.user.dto;

import io.moer.booking.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfileResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String profileImageUrl;
    private String marketingAgree;
    private int reservationCount;
    private int reviewCount;
    private LocalDateTime createdAt;

    public static CustomerProfileResponse from(User user, int reservationCount, int reviewCount) {
        return CustomerProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profileImageUrl(user.getProfileImageUrl())
                .marketingAgree(user.getMarketingAgree())
                .reservationCount(reservationCount)
                .reviewCount(reviewCount)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
