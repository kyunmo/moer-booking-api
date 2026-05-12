package io.moer.booking.common.exception;

import io.moer.booking.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Environment environment;

    /**
     * SECURITY (P3-2): 운영 환경에서는 내부 필드명 노출을 최소화.
     * - prod 프로필: 필드별 상세 details 제거. 일반 메시지만 반환.
     * - dev/local 프로필: 기존처럼 필드명 + 검증 메시지 노출 (개발 편의)
     */
    private boolean isProdProfile() {
        for (String active : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(active)) return true;
        }
        return false;
    }

    /**
     * BaseException (우리가 정의한 예외) 처리
     */
    @ExceptionHandler(BaseException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
        log.error("BaseException: {}", e.getMessage(), e);

        ErrorCode errorCode = e.getErrorCode();
        ApiResponse<Void> response = ApiResponse.error(
                errorCode.getCode(),
                errorCode.getMessage(),
                e.getDetails()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }

    /**
     * Validation 실패 (@Valid, @Validated)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException: {}", e.getMessage());

        // SECURITY (P3-2): prod 에서는 필드명/내부 메시지 미노출
        Object details = buildValidationDetails(e.getBindingResult().getFieldErrors());

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.INVALID_INPUT_VALUE.getCode(),
                ErrorCode.INVALID_INPUT_VALUE.getMessage(),
                details
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * BindException 처리
     */
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        log.error("BindException: {}", e.getMessage());

        // SECURITY (P3-2): prod 에서는 필드명/내부 메시지 미노출
        Object details = buildValidationDetails(e.getBindingResult().getFieldErrors());

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.INVALID_INPUT_VALUE.getCode(),
                ErrorCode.INVALID_INPUT_VALUE.getMessage(),
                details
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Validation 에러 상세 정보 빌더.
     * - dev/local: 필드명 + 메시지 매핑 (개발 편의)
     * - prod: 단순 메시지 ("입력값을 확인해주세요"), 필드명 누설 방지
     */
    private Object buildValidationDetails(java.util.List<FieldError> fieldErrors) {
        if (isProdProfile()) {
            return "입력값을 확인해주세요";
        }
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : fieldErrors) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return errors;
    }

    /**
     * 타입 불일치
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        log.error("MethodArgumentTypeMismatchException: {}", e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.INVALID_TYPE_VALUE.getCode(),
                ErrorCode.INVALID_TYPE_VALUE.getMessage(),
                String.format("%s는 %s 타입이어야 합니다",
                        e.getName(),
                        e.getRequiredType().getSimpleName())
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 지원하지 않는 HTTP 메서드
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        log.error("HttpRequestMethodNotSupportedException: {}", e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.METHOD_NOT_ALLOWED.getCode(),
                ErrorCode.METHOD_NOT_ALLOWED.getMessage(),
                String.format("지원하는 메서드: %s", String.join(", ", e.getSupportedMethods()))
        );

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(response);
    }

    /**
     * 그 외 모든 예외
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected Exception: {}", e.getMessage(), e);

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}