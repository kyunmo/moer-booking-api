package io.moer.booking.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.ConsumptionProbe;
import io.moer.booking.common.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

/**
 * SECURITY (P1-3): 인증/공개 엔드포인트 Rate Limiting Filter.
 *
 * 요청 URI 와 HTTP 메서드로 정책을 매칭하고, IP 기반 키를 생성하여 RateLimiterService 호출.
 * 한도 초과 시 429 Too Many Requests 응답 + 남은 시간(헤더) 안내.
 *
 * 적용 대상:
 * - POST /api/auth/login                  → LOGIN
 * - POST /api/auth/register               → REGISTER
 * - POST /api/auth/forgot-password        → FORGOT_PASSWORD
 * - GET  /api/users/check-email           → CHECK_EMAIL
 * - GET  /api/public/businesses(?...)     → PUBLIC_SEARCH
 * - GET  /api/public/help(?...)           → PUBLIC_SEARCH
 *
 * 멀티 인스턴스 배포 시 RateLimiterService 백엔드를 Redis 등으로 교체 필요.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // JWT 필터(기본) 보다 약간 늦지만 인증 처리 전에 동작하도록
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    // ObjectMapper 는 Spring 컨테이너 의존하지 않고 자체 인스턴스 사용 (Spring Boot 4 starter 구조 호환)
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    private static final List<RateLimitRule> RULES = List.of(
            new RateLimitRule(HttpMethod.POST, "/api/auth/login", RateLimiterService.RateLimitPolicy.LOGIN),
            new RateLimitRule(HttpMethod.POST, "/api/auth/register", RateLimiterService.RateLimitPolicy.REGISTER),
            new RateLimitRule(HttpMethod.POST, "/api/auth/forgot-password", RateLimiterService.RateLimitPolicy.FORGOT_PASSWORD),
            new RateLimitRule(HttpMethod.POST, "/api/auth/reset-password", RateLimiterService.RateLimitPolicy.FORGOT_PASSWORD),
            new RateLimitRule(HttpMethod.GET, "/api/users/check-email", RateLimiterService.RateLimitPolicy.CHECK_EMAIL),
            new RateLimitRule(HttpMethod.POST, "/api/users", RateLimiterService.RateLimitPolicy.REGISTER),
            new RateLimitRule(HttpMethod.GET, "/api/public/businesses", RateLimiterService.RateLimitPolicy.PUBLIC_SEARCH, true),
            new RateLimitRule(HttpMethod.GET, "/api/public/help", RateLimiterService.RateLimitPolicy.PUBLIC_SEARCH, true)
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        RateLimitRule matched = findRule(request);
        if (matched == null) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        String key = matched.policy.name() + ":ip:" + clientIp;

        ConsumptionProbe probe = rateLimiterService.tryConsume(key, matched.policy);

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSec = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        log.warn("[RateLimit] Blocked: method={} uri={} ip={} policy={} retryAfter={}s",
                request.getMethod(), request.getRequestURI(), clientIp, matched.policy, retryAfterSec);

        writeTooManyRequests(response, retryAfterSec);
    }

    private RateLimitRule findRule(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        for (RateLimitRule rule : RULES) {
            if (!rule.method.matches(method)) continue;
            if (rule.prefixMatch ? uri.startsWith(rule.uri) : uri.equals(rule.uri)) {
                return rule;
            }
        }
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        // 프록시/로드밸런서 환경 대응: X-Forwarded-For 우선 (가장 왼쪽 IP)
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response, long retryAfterSec) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSec));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<?> body = ApiResponse.error("TOO_MANY_REQUESTS",
                "요청이 너무 많습니다. " + retryAfterSec + "초 후 다시 시도해주세요.");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private static final class RateLimitRule {
        final HttpMethod method;
        final String uri;
        final RateLimiterService.RateLimitPolicy policy;
        final boolean prefixMatch;

        RateLimitRule(HttpMethod method, String uri, RateLimiterService.RateLimitPolicy policy) {
            this(method, uri, policy, false);
        }

        RateLimitRule(HttpMethod method, String uri, RateLimiterService.RateLimitPolicy policy, boolean prefixMatch) {
            this.method = method;
            this.uri = uri;
            this.policy = policy;
            this.prefixMatch = prefixMatch;
        }
    }

    /**
     * 외부에서 정책을 직접 활용할 수 있도록 헬퍼 함수 노출.
     */
    public <T> T runWithLimit(String key, RateLimiterService.RateLimitPolicy policy, Function<ConsumptionProbe, T> fn) {
        ConsumptionProbe probe = rateLimiterService.tryConsume(key, policy);
        return fn.apply(probe);
    }
}
