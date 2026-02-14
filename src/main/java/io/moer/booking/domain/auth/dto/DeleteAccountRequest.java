package io.moer.booking.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAccountRequest {
    @NotBlank(message = "비밀번호를 입력해주세요")
    private String password;

    private String reason;
}
