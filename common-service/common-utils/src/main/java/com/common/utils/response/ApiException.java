package com.common.utils.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import jakarta.annotation.Nullable;

/**
 * 业务异常
 */
@Getter
public class ApiException extends RuntimeException {

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    @Nullable
    private final Object data;

    public ApiException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(),
                HttpStatus.valueOf(errorCode.getHttpCode()), null);
    }

    public ApiException(int code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST, null);
    }

    public ApiException(int code, String message, HttpStatus httpStatus) {
        this(code, message, httpStatus, null);
    }

    public ApiException(int code, String message, HttpStatus httpStatus, @Nullable Object data) {
        super(message);
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
        this.data = data;
    }

    public int getHttpCode() {
        return httpStatus != null ? httpStatus.value() : 400;
    }
}
