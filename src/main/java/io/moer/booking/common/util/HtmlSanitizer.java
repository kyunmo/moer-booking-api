package io.moer.booking.common.util;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

/**
 * XSS 방어용 HTML 정화 유틸.
 *
 * SECURITY (P1-5): 사용자 입력 텍스트(리뷰 content, 문의 content, 매장 설명 등)에
 * 포함될 수 있는 악성 HTML/JavaScript 를 저장 시점에 제거.
 *
 * 두 가지 모드:
 * - {@link #plainText(String)} : 모든 HTML 태그 제거, 순수 텍스트만 반환
 * - {@link #safeRichText(String)} : 안전한 서식 태그만 허용 (강조, 단락 등)
 */
public final class HtmlSanitizer {

    /**
     * 모든 HTML 태그를 제거하고 텍스트만 남기는 정책.
     * (안전한 기본값 — 대부분의 필드에 적합)
     */
    private static final PolicyFactory PLAIN_TEXT_POLICY = new HtmlPolicyBuilder().toFactory();

    /**
     * 제한된 서식 허용 정책.
     * - 강조, 단락, 줄바꿈, 링크(https/http만)
     * - 스크립트 / 스타일 / 이벤트 핸들러 / 위험 속성 차단
     */
    private static final PolicyFactory SAFE_RICH_POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.LINKS);

    private HtmlSanitizer() {}

    /**
     * 모든 HTML 태그 제거. 순수 텍스트만 반환.
     * null 안전 (null → null).
     */
    public static String plainText(String input) {
        if (input == null) return null;
        return PLAIN_TEXT_POLICY.sanitize(input);
    }

    /**
     * 안전한 서식 태그(b, i, p, br, a, ul/ol/li 등)만 허용.
     * null 안전 (null → null).
     */
    public static String safeRichText(String input) {
        if (input == null) return null;
        return SAFE_RICH_POLICY.sanitize(input);
    }
}
