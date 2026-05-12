package io.moer.booking.common.config;

import io.moer.booking.common.security.CustomAuthorizationRequestResolver;
import io.moer.booking.common.security.CustomOAuth2UserService;
import io.moer.booking.common.security.CustomUserDetailsService;
import io.moer.booking.common.security.JwtAuthenticationFilter;
import io.moer.booking.common.security.JwtTokenProvider;
import io.moer.booking.common.security.OAuth2AuthenticationFailureHandler;
import io.moer.booking.common.security.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenProvider tokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauth2FailureHandler;
    private final PasswordEncoder passwordEncoder;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final Environment environment;

    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:8080,http://localhost:5173}")
    private String allowedOrigins;

    /**
     * 운영(prod) 프로필 여부.
     * - true: Swagger/API Docs 비활성 + Strict CSP
     * - false: 개발 편의를 위해 Swagger 허용 + 완화된 CSP
     */
    private boolean isProdProfile() {
        for (String active : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(active)) return true;
        }
        return false;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenProvider, userDetailsService);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // SECURITY (P0-5 관련): 와일드카드 대신 필요한 헤더만 화이트리스트
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Accept", "X-Requested-With", "X-Customer-Token"
        ));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean prod = isProdProfile();

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // SECURITY (P0-5): HTTP 보안 헤더 적용
                // - HSTS: HTTPS 강제 (max-age 1년, 서브도메인 포함)
                // - X-Frame-Options: DENY (Clickjacking 방지)
                // - X-Content-Type-Options: nosniff (MIME 스니핑 방지)
                // - Referrer-Policy: same-origin
                // - CSP: 운영은 strict default-src 'self', 개발은 Swagger UI 호환 위해 완화
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                        // SECURITY (P0-5 / P3-8): Content-Security-Policy
                        // - prod: strict default-src 'self', form-action self, upgrade-insecure-requests.
                        //         unsafe-inline 완전 제거.
                        // - dev/local: Swagger UI inline script/style 호환을 위해 일부 허용.
                        //         향후 Swagger UI nonce 통합 또는 별도 admin 호스팅 분리 시 strict 전환 가능.
                        .contentSecurityPolicy(csp -> csp.policyDirectives(prod
                                ? "default-src 'self'; "
                                + "script-src 'self'; "
                                + "style-src 'self'; "
                                + "img-src 'self' data: blob:; "
                                + "font-src 'self' data:; "
                                + "connect-src 'self'; "
                                + "frame-ancestors 'none'; "
                                + "object-src 'none'; "
                                + "base-uri 'self'; "
                                + "form-action 'self'; "
                                + "upgrade-insecure-requests"
                                : "default-src 'self'; "
                                + "script-src 'self' 'unsafe-inline'; "
                                + "style-src 'self' 'unsafe-inline'; "
                                + "img-src 'self' data: blob:; "
                                + "frame-ancestors 'none'; "
                                + "object-src 'none'; "
                                + "base-uri 'self'"))
                        // SECURITY (P3-8): Permissions-Policy — 불필요한 브라우저 API 차단
                        .permissionsPolicyHeader(pp -> pp.policy(
                                "camera=(), microphone=(), geolocation=(self), payment=(self), "
                                + "usb=(), magnetometer=(), gyroscope=(), accelerometer=()"))
                )

                .authorizeHttpRequests(auth -> {
                    auth
                        // 인증 불필요 (Public)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/error").permitAll()

                        // SECURITY (P2-5): Actuator — health/info 만 비인증 허용, 나머지는 관리자
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // OAuth2 엔드포인트 (Phase 3: SNS 로그인)
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                        // Phase 3: 고객용 Public API (비인증)
                        .requestMatchers("/api/public/**").permitAll()

                        // 업로드 파일 접근 (Public)
                        .requestMatchers("/uploads/**").permitAll();

                    // SECURITY (P0-6): Swagger/API 문서는 prod 프로필에서 비활성 + 차단
                    // springdoc.api-docs.enabled=false 설정과 함께 SecurityFilterChain 에서도 deny
                    if (prod) {
                        auth.requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).denyAll();
                    } else {
                        auth.requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll();
                    }

                    auth
                        // Customer 전용 (Phase 3 Addendum: 고객 인증)
                        // OWNER 계정이 고객 사이트에서 OAuth 로그인 시에도 접근 가능하도록 authenticated()로 변경
                        .requestMatchers("/api/customer/**").authenticated()

                        // 나머지는 인증 필요
                        .anyRequest().authenticated();
                })
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/oauth2/authorize")
                                .authorizationRequestResolver(
                                        new CustomAuthorizationRequestResolver(clientRegistrationRepository)))
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/login/oauth2/code/*"))
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}