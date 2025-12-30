package io.moer.booking.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret = "moer-booking-system-secret-key-change-this-in-production-minimum-256-bits";
    private Long accessTokenExpiration = 3600000L;  // 1시간 (밀리초)
    private Long refreshTokenExpiration = 604800000L;  // 7일 (밀리초)
}