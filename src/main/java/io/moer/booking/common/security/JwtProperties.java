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
    // SECURITY: 기본값 없음. 반드시 환경변수 JWT_SECRET 으로 주입.
    // 검증은 io.moer.booking.common.config.SecretsValidator 에서 수행.
    private String secret;
    private Long accessTokenExpiration = 3600000L;  // 1시간 (밀리초)
    private Long refreshTokenExpiration = 604800000L;  // 7일 (밀리초)
}