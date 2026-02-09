package io.moer.booking.common.security;

import io.moer.booking.domain.auth.RefreshToken;
import io.moer.booking.domain.auth.repository.RefreshTokenRepository;
import io.moer.booking.domain.user.User;
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

/**
 * OAuth2 로그인 성공 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

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

            // 프론트엔드로 리다이렉트 (토큰 포함)
            String targetUrl = UriComponentsBuilder
                    .fromUriString(redirectUri)
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build().toUriString();

            log.info("OAuth2 login success: userId={}, redirecting to: {}", user.getId(), redirectUri);

            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            log.error("Failed to handle OAuth2 authentication success: userId={}, error={}",
                    user.getId(), e.getMessage(), e);
            throw new IOException("OAuth2 인증 처리 중 오류가 발생했습니다", e);
        }
    }
}
