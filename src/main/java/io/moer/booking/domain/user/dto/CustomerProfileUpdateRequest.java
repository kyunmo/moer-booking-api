package io.moer.booking.domain.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileUpdateRequest {
    @Size(min = 1, max = 50, message = "이름은 1~50자 이내여야 합니다")
    private String name;

    @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "올바른 전화번호 형식이 아닙니다")
    private String phone;

    @Pattern(regexp = "^[YN]$", message = "마케팅 수신 동의는 Y 또는 N이어야 합니다")
    private String marketingAgree;
}
