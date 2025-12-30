package io.moer.booking.domain.user.dto;

import io.moer.booking.domain.user.UserRole;
import io.moer.booking.domain.user.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchCondition {

    private String keyword;      // 이름 또는 이메일 검색
    private UserRole role;       // 역할 필터
    private UserStatus status;   // 상태 필터
    private Long businessId;     // 매장 필터
    private int page = 1;        // 페이지 번호 (1부터 시작)
    private int size = 20;       // 페이지당 개수

    // MyBatis에서 사용할 offset 계산
    public int getOffset() {
        return (page - 1) * size;
    }
}