package com.common.utils.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import jakarta.annotation.Nullable;

/**
 * 统一 API 响应封装
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;

    @Nullable
    private T data;

    private HttpStatus status;

    @Nullable
    private ApiError error;

    /* 成功响应 */

    public static <T> ApiResponse<T> success(@Nullable T data) {
        return ApiResponse.<T>builder().success(true).data(data).status(HttpStatus.OK).build();
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /* 失败响应 */

    public static <T> ApiResponse<T> failure(ApiException e) {
        return ApiResponse.<T>builder()
                .success(false).status(HttpStatus.valueOf(e.getHttpCode()))
                .error(ApiError.of(e.getCode(), e.getMessage())).build();
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .success(false).status(HttpStatus.valueOf(errorCode.getHttpCode()))
                .error(ApiError.from(errorCode)).build();
    }

    public static <T> ApiResponse<T> failure(int code, String message, HttpStatus status) {
        return ApiResponse.<T>builder()
                .success(false).status(status).error(ApiError.of(code, message)).build();
    }

    public static <T> ApiResponse<T> failure(int code, String message) {
        return failure(code, message, HttpStatus.BAD_REQUEST);
    }

    /* 工具方法 */

    public int getHttpCode() {
        return status != null ? status.value() : 200;
    }

    public String getHttpMessage() {
        return status != null ? status.getReasonPhrase() : HttpStatus.OK.getReasonPhrase();
    }

    /**
     * 错误信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiError {
        private int code;
        private String message;

        public static ApiError of(int code, String message) {
            return new ApiError(code, message);
        }

        public static ApiError from(ErrorCode errorCode) {
            return new ApiError(errorCode.getCode(), errorCode.getMessage());
        }
    }
}
