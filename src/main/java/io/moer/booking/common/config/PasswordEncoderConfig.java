package io.moer.booking.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordEncoder 설정.
 * SecurityConfig 와의 순환 의존성 방지를 위해 분리.
 *
 * SECURITY (P2-1): BCrypt cost factor 12 (2^12 = 4096 라운드).
 * - 기본값 10 대비 약 4배 느림, GPU 기반 brute-force 공격에 더 강인.
 * - NIST/OWASP 권장값(>=12) 만족.
 * - Refresh Token 해시(P1-1) 에도 동일 인코더 사용 → 부수효과로 토큰 해시 강도도 함께 상향.
 */
@Configuration
public class PasswordEncoderConfig {

    private static final int BCRYPT_STRENGTH = 12;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }
}
