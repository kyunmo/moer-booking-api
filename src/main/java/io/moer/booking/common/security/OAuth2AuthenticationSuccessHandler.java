package io.moer.booking.common.security;

import io.moer.booking.domain.auth.RefreshToken;
import io.moer.booking.domain.auth.repository.RefreshTokenRepository;
import io.moer.booking.domain.user.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * OAuth2 로그인 성공 핸들러
 *
 * loginType 쿠키에 따라 리다이렉트 URI를 분기한다:
 * - loginType=customer: customerRedirectUri 로 리다이렉트
 * - 그 외: redirectUri(관리자용)로 리다이렉트
 *
 * 리다이렉트 시 isNewUser 플래그를 함께 전달한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Value("${app.oauth2.customer-redirect-uri:${app.oauth2.redirect-uri}}")
    private String customerRedirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oauth2User.getUser();

        try {
            // JWT 토큰 생성
            String accessToken = tokenProvider.generateAccessToken(user);
            String refreshToken = tokenProvider.generateRefreshToken(user);

            // Refresh Token 저장
            RefreshToken refreshTokenEntity = RefreshToken.builder()
                    .userId(user.getId())
                    .token(refreshToken)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
            refreshTokenRepository.deleteByUserId(user.getId());
            refreshTokenRepository.save(refreshTokenEntity);

            // loginType 쿠키 읽기 및 삭제
            String loginType = readAndDeleteLoginTypeCookie(request, response);

            // 신규 사용자 판별 (생성 후 60초 이내)
            boolean isNewUser = isNewlyCreatedUser(user);

            // loginType에 따라 리다이렉트 URI 결정
            String targetRedirectUri = "customer".equals(loginType)
                    ? customerRedirectUri
                    : redirectUri;

            // 프론트엔드로 리다이렉트 (토큰 + loginType + isNewUser 포함)
            String effectiveLoginType = loginType != null ? loginType : "admin";
            String targetUrl = UriComponentsBuilder
                    .fromUriString(targetRedirectUri)
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .queryParam("loginType", effectiveLoginType)
                    .queryParam("isNewUser", isNewUser)
                    .build().toUriString();

            log.info("OAuth2 login success: userId={}, loginType={}, isNewUser={}, redirecting to: {}",
                    user.getId(), loginType, isNewUser, targetRedirectUri);

            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            log.error("Failed to handle OAuth2 authentication success: userId={}, error={}",
                    user.getId(), e.getMessage(), e);
            throw new IOException("OAuth2 인증 처리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * moer_login_type 쿠키를 읽고 삭제한다.
     *
     * @return loginType 값 (없으면 null)
     */
    private String readAndDeleteLoginTypeCookie(HttpServletRequest request, HttpServletResponse response) {
        String loginType = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("moer_login_type".equals(cookie.getName())) {
                    loginType = cookie.getValue();
                    break;
                }
            }
        }

        // 쿠키 삭제 (maxAge=0으로 설정)
        Cookie deleteCookie = new Cookie("moer_login_type", "");
        deleteCookie.setPath("/");
        deleteCookie.setMaxAge(0);
        deleteCookie.setHttpOnly(true);
        response.addCookie(deleteCookie);

        return loginType;
    }

    /**
     * 사용자가 최근 60초 이내에 생성되었는지 확인한다.
     * createdAt이 null이면 신규로 간주한다.
     */
    private boolean isNewlyCreatedUser(User user) {
        if (user.getCreatedAt() == null) {
            return true;
        }
        long secondsSinceCreation = ChronoUnit.SECONDS.between(user.getCreatedAt(), LocalDateTime.now());
        return secondsSinceCreation <= 60;
    }
}
