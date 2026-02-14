package io.moer.booking.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.auth.SnsAccount;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SnsAccountResponse {
    private String provider;
    private String email;
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime connectedAt;

    public static SnsAccountResponse from(SnsAccount snsAccount) {
        return SnsAccountResponse.builder()
                .provider(snsAccount.getProvider().name())
                .email(snsAccount.getEmail())
                .name(snsAccount.getName())
                .connectedAt(snsAccount.getCreatedAt())
                .build();
    }
}
