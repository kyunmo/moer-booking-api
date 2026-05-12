package io.moer.booking.common.util;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.ErrorCode;

import java.util.Set;

/**
 * 비밀번호 정책 검증.
 *
 * SECURITY (P3-7): NIST SP 800-63B Memorized Secret Authenticators 기반.
 * - 최소 길이: 10자 (NIST 권장 8자 이상, 강화)
 * - 최대 길이: 128자
 * - 흔한 비밀번호 / 사전 단어 차단 (간이 블랙리스트)
 * - 복잡성 강제(대/소/숫자/특수) 보다는 길이와 블랙리스트로 충분 — NIST 800-63B 권장
 *
 * 가입(register), 비밀번호 변경, 재설정 시 호출.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 128;

    /**
     * 자주 사용되어 위험한 비밀번호 패턴 (간이 블랙리스트).
     * 운영 시에는 HIBP(Have I Been Pwned) API / 다운로드 리스트 연동 권장.
     */
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "password123", "password1234", "password12345",
            "qwerty", "qwerty123", "qwertyuiop",
            "12345678", "123456789", "1234567890",
            "abcdefghij", "abc12345",
            "admin", "administrator", "admin123", "admin1234",
            "letmein", "welcome", "welcome123",
            "iloveyou", "monkey", "dragon",
            "passw0rd", "p@ssw0rd", "p@ssword",
            "moer", "moer123", "moer1234", "moer2026"
    );

    private PasswordPolicy() {}

    /**
     * 비밀번호 정책 검증. 위반 시 BusinessException.
     *
     * @param rawPassword 검증할 비밀번호 (평문)
     * @param email       (옵션) 사용자 이메일 — 이메일 로컬파트가 비밀번호에 포함되면 차단
     */
    public static void validate(String rawPassword, String email) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "비밀번호를 입력해주세요");
        }

        int len = rawPassword.length();
        if (len < MIN_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "비밀번호는 최소 " + MIN_LENGTH + "자 이상이어야 합니다");
        }
        if (len > MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "비밀번호는 최대 " + MAX_LENGTH + "자 이내여야 합니다");
        }

        // 공통 패스워드 차단 (대소문자 무시)
        String lower = rawPassword.toLowerCase();
        if (COMMON_PASSWORDS.contains(lower)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "너무 흔한 비밀번호입니다. 더 안전한 비밀번호를 사용해주세요");
        }

        // 단일 문자 반복 (예: "aaaaaaaaaa") 차단
        if (isRepeatedChar(rawPassword)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "단일 문자 반복은 사용할 수 없습니다");
        }

        // 이메일 로컬파트 포함 차단
        if (email != null && !email.isBlank()) {
            int at = email.indexOf('@');
            String local = at > 0 ? email.substring(0, at) : email;
            if (local.length() >= 4 && lower.contains(local.toLowerCase())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                        "이메일 정보를 비밀번호에 포함할 수 없습니다");
            }
        }
    }

    private static boolean isRepeatedChar(String s) {
        if (s.isEmpty()) return false;
        char first = s.charAt(0);
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) != first) return false;
        }
        return true;
    }
}
