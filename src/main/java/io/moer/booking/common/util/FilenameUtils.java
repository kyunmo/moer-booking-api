package io.moer.booking.common.util;

/**
 * 파일명 정화 유틸.
 *
 * SECURITY (P1-9): 사용자 디바이스의 원본 파일명에 경로 정보(C:\...), 제어 문자,
 * 비정상 길이 등이 포함될 수 있으므로 저장/로깅 전 정화 필요.
 *
 * - Path traversal 패턴 (../, ..\) 제거
 * - 경로 구분자 (/ \) 제거
 * - 제어 문자 제거
 * - 길이 제한 (255)
 * - 안전한 ASCII + 한글만 허용 (그 외 특수문자는 _)
 */
public final class FilenameUtils {

    private static final int MAX_FILENAME_LENGTH = 255;

    private FilenameUtils() {}

    /**
     * 원본 파일명을 안전한 형태로 정화.
     * null/빈 문자열 → "unnamed"
     */
    public static String sanitize(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "unnamed";
        }

        // 경로 구분자로 분리하여 마지막 세그먼트만 사용 (path traversal 방어)
        String basename = originalFilename
                .replace('\\', '/'); // 윈도우 경로를 슬래시로 통일
        int lastSlash = basename.lastIndexOf('/');
        if (lastSlash >= 0) {
            basename = basename.substring(lastSlash + 1);
        }

        // ".." 같은 위험 패턴 제거 및 제어 문자/위험 문자 정화
        // 허용: 한글, 영문, 숫자, 점, 하이픈, 언더스코어, 공백
        String cleaned = basename
                .replaceAll("\\.{2,}", ".")  // 연속 점 → 단일 점
                .replaceAll("[\\p{Cntrl}]", "") // 제어 문자 제거
                .replaceAll("[^\\p{L}\\p{N}\\._\\- ]", "_"); // 그 외 문자 → _

        // 양 끝 공백/점 제거 (Windows 호환성)
        cleaned = cleaned.trim().replaceAll("^[.]+|[.]+$", "");

        if (cleaned.isBlank()) return "unnamed";

        // 길이 제한
        if (cleaned.length() > MAX_FILENAME_LENGTH) {
            cleaned = cleaned.substring(0, MAX_FILENAME_LENGTH);
        }
        return cleaned;
    }
}
