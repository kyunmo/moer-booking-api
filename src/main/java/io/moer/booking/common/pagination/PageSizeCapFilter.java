package io.moer.booking.common.pagination;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * SECURITY (P2-2): 페이지 size 파라미터 글로벌 상한 강제.
 *
 * 모든 HTTP 요청의 query string 에서 'size' 파라미터를 가로채 {@link #MAX_PAGE_SIZE} 로 캡.
 * - 사용자가 size=100000 같은 값을 전송해 메모리/DB 부하를 일으키는 것을 차단.
 * - 음수/0/비숫자는 기본값 처리는 컨트롤러 단에서 그대로 수행 (필터는 상한만 강제).
 * - 컨트롤러/DTO 마다 @Max 어노테이션을 일일이 붙이지 않아도 일관 적용.
 *
 * 적용 범위: 모든 요청. Body(JSON) 의 size 는 다루지 않음 (대부분의 페이징은 query 사용).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20) // RateLimitFilter(+10) 다음에 동작
public class PageSizeCapFilter extends OncePerRequestFilter {

    public static final int MAX_PAGE_SIZE = 100;
    private static final String SIZE_PARAM = "size";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String raw = request.getParameter(SIZE_PARAM);
        if (raw == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            int requested = Integer.parseInt(raw.trim());
            if (requested > MAX_PAGE_SIZE) {
                log.debug("[PageSizeCap] capping size {} -> {} (uri={})", requested, MAX_PAGE_SIZE, request.getRequestURI());
                chain.doFilter(new CappedSizeRequest(request, MAX_PAGE_SIZE), response);
                return;
            }
        } catch (NumberFormatException ignore) {
            // 숫자 아닌 입력은 컨트롤러 단 검증에 위임
        }

        chain.doFilter(request, response);
    }

    /**
     * size 파라미터만 캡된 값으로 치환하는 래퍼.
     */
    private static class CappedSizeRequest extends HttpServletRequestWrapper {
        private final String cappedValue;

        CappedSizeRequest(HttpServletRequest request, int capped) {
            super(request);
            this.cappedValue = String.valueOf(capped);
        }

        @Override
        public String getParameter(String name) {
            if (SIZE_PARAM.equals(name)) return cappedValue;
            return super.getParameter(name);
        }

        @Override
        public String[] getParameterValues(String name) {
            if (SIZE_PARAM.equals(name)) return new String[]{cappedValue};
            return super.getParameterValues(name);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> original = super.getParameterMap();
            if (!original.containsKey(SIZE_PARAM)) return original;
            Map<String, String[]> copy = new LinkedHashMap<>(original);
            copy.put(SIZE_PARAM, new String[]{cappedValue});
            return Collections.unmodifiableMap(copy);
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return super.getParameterNames();
        }
    }
}
