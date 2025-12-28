package io.moer.booking.common.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
public class TestController {

    /**
     * 정상 응답 테스트
     */
    @GetMapping("/success")
    public ApiResponse<String> testSuccess() {
        return ApiResponse.success("테스트 성공!");
    }

    /**
     * BusinessException 테스트
     */
    @GetMapping("/business-error")
    public ApiResponse<Void> testBusinessError() {
        throw new BusinessException(ErrorCode.BUSINESS_NOT_FOUND);
    }

    /**
     * EntityNotFoundException 테스트
     */
    @GetMapping("/not-found")
    public ApiResponse<Void> testNotFound() {
        throw new EntityNotFoundException("User", 999L);
    }

    /**
     * Validation 테스트
     */
    @PostMapping("/validation")
    public ApiResponse<TestRequest> testValidation(@Valid @RequestBody TestRequest request) {
        return ApiResponse.success(request);
    }

    /**
     * 예상치 못한 예외 테스트
     */
    @GetMapping("/unexpected-error")
    public ApiResponse<Void> testUnexpectedError() {
        throw new RuntimeException("예상치 못한 에러 발생!");
    }

    @Data
    static class TestRequest {
        @NotBlank(message = "이름은 필수입니다")
        private String name;

        @Email(message = "올바른 이메일 형식이 아닙니다")
        @NotBlank(message = "이메일은 필수입니다")
        private String email;
    }
}