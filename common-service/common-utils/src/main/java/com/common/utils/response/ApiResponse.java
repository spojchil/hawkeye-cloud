package com.common.utils.response;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.springframework.http.HttpStatus;
import jakarta.annotation.Nullable;

/**
 * 统一API响应封装
 */
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)  // 禁止外部直接new对象
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private boolean success;

    @Nullable
    private T data;

    private HttpStatus status;

    @Nullable
    private ApiError error;

    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(@Nullable T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .status(HttpStatus.OK)
                .build();
    }

    public static <T> ApiResponse<T> success(@Nullable T data, HttpStatus status) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .status(status)
                .build();
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> failure(ApiException e) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(HttpStatus.valueOf(e.getHttpCode()))
                .error(ApiError.of(e.getCode(), e.getMessage()))
                .build();
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(HttpStatus.valueOf(errorCode.getHttpCode()))
                .error(ApiError.of(errorCode.getCode(), errorCode.getMessage()))
                .build();
    }

    public static <T> ApiResponse<T> failure(int code, String message) {
        return failure(code, message, HttpStatus.BAD_REQUEST);
    }

    public static <T> ApiResponse<T> failure(int code, String message, HttpStatus status) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(status)
                .error(ApiError.of(code, message))
                .build();
    }

    public static <T> ApiResponse<T> failure(ApiError error) {
        return failure(error, HttpStatus.BAD_REQUEST);
    }

    public static <T> ApiResponse<T> failure(ApiError error, HttpStatus status) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(status)
                .error(error)
                .build();
    }

    /**
     * 常见 HTTP 错误响应
     */
    public static <T> ApiResponse<T> badRequest(int code, String message) {
        return failure(code, message, HttpStatus.BAD_REQUEST);
    }

    public static <T> ApiResponse<T> unauthorized(int code, String message) {
        return failure(code, message, HttpStatus.UNAUTHORIZED);
    }

    public static <T> ApiResponse<T> forbidden(int code, String message) {
        return failure(code, message, HttpStatus.FORBIDDEN);
    }

    public static <T> ApiResponse<T> notFound(int code, String message) {
        return failure(code, message, HttpStatus.NOT_FOUND);
    }

    public static <T> ApiResponse<T> internalServerError(int code, String message) {
        return failure(code, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 重定向响应
     */
    public static <T> ApiResponse<T> movedPermanently(@Nullable T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .data(data)
                .status(HttpStatus.MOVED_PERMANENTLY)
                .error(ApiError.of(301, "Resource has been moved permanently"))
                .build();
    }

    /**
     * 工具方法 - 获取 HTTP 状态码
     */
    public int getHttpCode() {
        return status != null ? status.value() : 200;
    }

    /**
     * 工具方法 - 获取 HTTP 状态描述
     */
    public String getHttpMessage() {
        return status != null ? status.getReasonPhrase() : HttpStatus.OK.getReasonPhrase();
    }

    /**
     * 错误信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiError {
        private int code;
        private String message;

        public static ApiError of(int code, String message) {
            return ApiError.builder()
                    .code(code)
                    .message(message)
                    .build();
        }

        public static ApiError from(ErrorCode errorCode) {
            return of(errorCode.getCode(), errorCode.getMessage());
        }
    }
}