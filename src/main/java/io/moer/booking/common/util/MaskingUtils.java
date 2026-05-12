package io.moer.booking.common.util;

/**
 * PII (개인식별정보) 마스킹 유틸리티.
 *
 * SECURITY (P1-7): 로그 출력 / 응답 노출 시 민감 정보를 마스킹하여
 * 로그 수집 시스템 침해 시 PII 유출 영향을 최소화.
 *
 * 마스킹 규칙:
 * - 전화번호: 01012345678 -> 010****5678
 * - 이메일: user@example.com -> u***@example.com
 * - 이름: 홍길동 -> 홍*동, 길동 -> 길*, 김 -> *
 * - 일반 텍스트: 첫 1~2글자 + ***
 */
public final class MaskingUtils {

    private MaskingUtils() {}

    /**
     * 전화번호 마스킹. 중간 4자리를 * 로 대체.
     * 예: 01012345678 -> 010****5678 / 0212345678 -> 02****5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return phone;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 7) return "***";
        int prefixLen = digits.length() >= 10 ? 3 : 2;
        int suffixLen = 4;
        if (digits.length() <= prefixLen + suffixLen) return "***";
        String prefix = digits.substring(0, prefixLen);
        String suffix = digits.substring(digits.length() - suffixLen);
        int maskLen = digits.length() - prefixLen - suffixLen;
        return prefix + "*".repeat(maskLen) + suffix;
    }

    /**
     * 이메일 마스킹.
     * 로컬 파트 첫 1글자만 노출, 나머지는 * 처리.
     * 예: user@example.com -> u***@example.com / a@b.c -> *@b.c
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) return email;
        int atIdx = email.indexOf('@');
        if (atIdx <= 0) return "***";
        String local = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (local.length() <= 1) return "*" + domain;
        return local.charAt(0) + "***" + domain;
    }

    /**
     * 이름 마스킹.
     * 한글 2글자: 첫글자 + *  (예: 홍길 -> 홍*)
     * 한글 3글자+: 첫글자 + * + 마지막글자 (예: 홍길동 -> 홍*동)
     * 한글 1글자: *
     */
    public static String maskName(String name) {
        if (name == null || name.isBlank()) return name;
        int len = name.length();
        if (len == 1) return "*";
        if (len == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*".repeat(len - 2) + name.charAt(len - 1);
    }

    /**
     * 토큰/시크릿 마스킹: 앞 8자리만 노출 + ... (디버그 추적용)
     */
    public static String maskToken(String token) {
        if (token == null || token.isBlank()) return token;
        if (token.length() <= 8) return "***";
        return token.substring(0, 8) + "...";
    }
}
