package io.moer.booking.common.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 로그인 실패 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        String errorMessage = exception.getMessage();
        log.error("OAuth2 authentication failed: {}", errorMessage, exception);

        // loginType 쿠키 읽기 및 삭제
        String loginType = readAndDeleteLoginTypeCookie(request, response);

        // 프론트엔드로 리다이렉트 (에러 정보 + loginType 포함)
        String targetUrl = UriComponentsBuilder
                .fromUriString(redirectUri)
                .queryParam("error", "true")
                .queryParam("message", errorMessage)
                .queryParam("loginType", loginType != null ? loginType : "admin")
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

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

        // 쿠키 삭제
        Cookie deleteCookie = new Cookie("moer_login_type", "");
        deleteCookie.setPath("/");
        deleteCookie.setMaxAge(0);
        deleteCookie.setHttpOnly(true);
        response.addCookie(deleteCookie);

        return loginType;
    }
}
