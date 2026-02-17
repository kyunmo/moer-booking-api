package io.moer.booking.common.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * OAuth2 인가 요청 시 loginType 쿠키를 설정하는 커스텀 리졸버.
 *
 * 프론트엔드에서 /oauth2/authorize/google?loginType=customer 와 같이
 * loginType 쿼리 파라미터를 전달하면, 해당 값을 moer_login_type 쿠키에 저장한다.
 * 이후 OAuth2 콜백에서 이 쿠키를 읽어 고객/관리자 분기 처리에 사용한다.
 */
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;

    public CustomAuthorizationRequestResolver(ClientRegistrationRepository repo) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorize");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        setLoginTypeCookie(request);
        return defaultResolver.resolve(request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        setLoginTypeCookie(request);
        return defaultResolver.resolve(request, clientRegistrationId);
    }

    /**
     * loginType 쿼리 파라미터가 있으면 moer_login_type 쿠키를 설정한다.
     * 쿠키는 HttpOnly, 5분(300초) 만료, path=/ 로 설정된다.
     */
    private void setLoginTypeCookie(HttpServletRequest request) {
        String loginType = request.getParameter("loginType");
        if (loginType != null && !loginType.isBlank()) {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletResponse response = attrs.getResponse();
                if (response != null) {
                    Cookie cookie = new Cookie("moer_login_type", loginType);
                    cookie.setPath("/");
                    cookie.setMaxAge(300); // 5분
                    cookie.setHttpOnly(true);
                    response.addCookie(cookie);
                }
            }
        }
    }
}
