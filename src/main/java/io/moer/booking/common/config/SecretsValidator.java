package io.moer.booking.common.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 부팅 시 운영에 필요한 필수 시크릿/설정이 주입되었는지 검증.
 * 누락된 경우 즉시 IllegalStateException 으로 부팅을 차단(Fail Fast).
 *
 * - 운영(prod) 프로필: 모든 시크릿 필수
 * - 로컬/개발(local, dev) 프로필: 비밀번호 등 핵심만 검증, OAuth 등은 경고만
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecretsValidator {

    private static final int JWT_SECRET_MIN_BYTES = 32; // 256 bit

    private final Environment environment;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleSecret;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret:}")
    private String naverSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret:}")
    private String kakaoSecret;

    @PostConstruct
    public void validate() {
        boolean isProd = isActiveProfile("prod");
        List<String> missing = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // === 핵심 시크릿 (모든 환경 필수) ===
        if (isBlank(jwtSecret)) {
            missing.add("JWT_SECRET (환경변수)");
        } else if (jwtSecret.getBytes().length < JWT_SECRET_MIN_BYTES) {
            missing.add("JWT_SECRET (32바이트 이상 필요, 현재 " + jwtSecret.getBytes().length + "바이트)");
        }

        if (isBlank(dbPassword)) {
            missing.add("DB_PASSWORD (환경변수)");
        }

        // === 운영 환경 추가 검증 ===
        if (isProd) {
            if (isBlank(mailPassword)) missing.add("MAIL_PASSWORD (환경변수, prod 필수)");
            if (isBlank(googleSecret)) warnings.add("GOOGLE_CLIENT_SECRET 미설정 (Google OAuth 비활성)");
            if (isBlank(naverSecret)) warnings.add("NAVER_CLIENT_SECRET 미설정 (Naver OAuth 비활성)");
            if (isBlank(kakaoSecret)) warnings.add("KAKAO_CLIENT_SECRET 미설정 (Kakao OAuth 비활성)");
        } else {
            if (isBlank(mailPassword)) warnings.add("MAIL_PASSWORD 미설정 (메일 발송 불가)");
        }

        // === 경고 출력 ===
        for (String w : warnings) {
            log.warn("[SecretsValidator] {}", w);
        }

        // === 누락 시 Fail Fast ===
        if (!missing.isEmpty()) {
            String message = "필수 시크릿/설정이 누락되었습니다 (" + String.join(", ", missing) + "). "
                    + "환경변수 또는 application-local.yml 에 설정 후 재시작하세요. "
                    + "예: docs/security/ENV-SETUP.md 참고";
            log.error("[SecretsValidator] {}", message);
            throw new IllegalStateException(message);
        }

        log.info("[SecretsValidator] 시크릿 검증 완료 (profile={})",
                String.join(",", environment.getActiveProfiles()));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isActiveProfile(String profile) {
        for (String active : environment.getActiveProfiles()) {
            if (profile.equalsIgnoreCase(active)) return true;
        }
        return false;
    }
}
