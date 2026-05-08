package com.common.utils.response;

import org.springframework.http.HttpStatus;

/**
 * 通用错误码枚举
 *
 * <p>号段约定：0=成功, 1xxx=参数/资源, 2xxx=认证/授权, 5xxx=服务端。
 * 各业务模块可扩展实现 ErrorCode 接口，使用不同号段避免冲突。</p>
 */
public enum CommonErrorCode implements ErrorCode {

    SUCCESS(0, "操作成功", 200),

    PARAM_INVALID(1001, "参数校验失败", 400),
    RESOURCE_NOT_FOUND(1002, "资源不存在", 404),
    RESOURCE_ALREADY_EXISTS(1003, "资源已存在", 409),
    OPERATION_DENIED(1004, "操作不允许", 403),
    DATA_CONFLICT(1005, "数据冲突", 409),

    UNAUTHORIZED(2001, "未认证", 401),
    FORBIDDEN(2002, "无权限", 403),
    TOKEN_EXPIRED(2003, "Token 已过期", 401),
    TOKEN_INVALID(2004, "Token 无效", 401),

    INTERNAL_ERROR(5001, "服务器内部错误", 500),
    SERVICE_UNAVAILABLE(5002, "服务暂不可用", 503);

    private final int code;
    private final String message;
    private final int httpCode;

    CommonErrorCode(int code, String message, int httpCode) {
        this.code = code;
        this.message = message;
        this.httpCode = httpCode;
    }

    @Override
    public int getCode() { return code; }

    @Override
    public String getMessage() { return message; }

    @Override
    public int getHttpCode() { return httpCode; }

    @Override
    public HttpStatus getHttpStatus() { return HttpStatus.valueOf(httpCode); }
}
