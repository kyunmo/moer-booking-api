package io.moer.booking.domain.auth.dto;

import io.moer.booking.domain.auth.SnsProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SNS에서 제공받은 사용자 정보
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnsUserInfo {
    private SnsProvider provider;
    private String providerUserId;
    private String email;
    private String name;
    private String profileImageUrl;
}
