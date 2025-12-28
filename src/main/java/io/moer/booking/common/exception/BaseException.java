package io.moer.booking.common.exception;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object details;

    public BaseException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public BaseException(ErrorCode errorCode, Object details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details;
    }
}