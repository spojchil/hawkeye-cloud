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
        this(errorCode.getCode(),
                errorCode.getMessage(),
                HttpStatus.valueOf(errorCode.getHttpCode()),
                null);
    }

    public ApiException(String message) {
        this(400, message, HttpStatus.BAD_REQUEST, null);
    }

    public ApiException(int code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST, null);
    }

    public ApiException(int code, String message, HttpStatus httpStatus) {
        this(code, message, httpStatus, null);
    }

    public ApiException(int code, String message, HttpStatus httpStatus, @Nullable Object data) {
        this(code, message, httpStatus, data, null);
    }

    public ApiException(int code, String message, HttpStatus httpStatus,
                        @Nullable Object data, @Nullable Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
        this.data = data;
    }

    /**
     * 便捷方法 - 获取 HTTP 状态码
     */
    public int getHttpCode() {
        return httpStatus != null ? httpStatus.value() : 400;
    }

    /**
     * 链式构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int code = 400;
        private String message = "Bad Request";
        private HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        private Object data;
        private Throwable cause;

        public Builder code(int code) {
            this.code = code;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder httpStatus(HttpStatus httpStatus) {
            this.httpStatus = httpStatus;
            return this;
        }

        public Builder httpCode(int httpCode) {
            this.httpStatus = HttpStatus.valueOf(httpCode);
            return this;
        }

        public Builder data(Object data) {
            this.data = data;
            return this;
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public ApiException build() {
            return new ApiException(code, message, httpStatus, data, cause);
        }
    }
}
