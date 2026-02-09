package io.moer.booking.domain.superadmin.dto;

import io.moer.booking.domain.user.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 역할 변경 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRoleRequest {
    @NotNull(message = "역할을 선택해주세요")
    private UserRole role;
}
