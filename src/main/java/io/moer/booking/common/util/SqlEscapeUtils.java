package io.moer.booking.common.util;

/**
 * SQL LIKE 검색용 와일드카드 이스케이프 유틸.
 *
 * SECURITY (P1-6): 사용자가 입력한 %, _ 가 SQL LIKE 와일드카드로 해석되어
 * 의도하지 않은 결과(전체 매칭) 또는 성능 저하를 유발하는 것을 방지.
 *
 * 사용법 (MyBatis XML):
 *   <bind name="kwEscaped"
 *         value="@io.moer.booking.common.util.SqlEscapeUtils@escapeLike(keyword)"/>
 *   ... LIKE '%' || #{kwEscaped} || '%' ESCAPE '\'
 *
 * 또는 Service 레이어에서 호출 후 SearchCondition 에 세팅.
 */
public final class SqlEscapeUtils {

    private SqlEscapeUtils() {}

    /**
     * LIKE 검색에 사용될 키워드에서 SQL 와일드카드(%, _, \)를 이스케이프.
     * null 안전. 이스케이프 문자는 백슬래시(\) 기준.
     * 반드시 ESCAPE '\' 절과 함께 사용할 것.
     */
    public static String escapeLike(String keyword) {
        if (keyword == null) return null;
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
