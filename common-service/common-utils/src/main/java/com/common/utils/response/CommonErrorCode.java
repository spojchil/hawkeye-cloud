package com.common.utils.response;

import org.springframework.http.HttpStatus;

/**
 * 通用错误码枚举，实现了 {@link ErrorCode} 接口。
 * <p>
 * <b>错误码号段约定：</b>
 * <table>
 *   <tr><th>号段</th><th>含义</th><th>示例</th></tr>
 *   <tr><td>0</td><td>成功</td><td>SUCCESS</td></tr>
 *   <tr><td>1xxx</td><td>请求参数 / 资源 / 操作错误</td><td>PARAM_INVALID(1001), RESOURCE_NOT_FOUND(1002)</td></tr>
 *   <tr><td>2xxx</td><td>认证 / 授权相关错误</td><td>UNAUTHORIZED(2001), TOKEN_EXPIRED(2003)</td></tr>
 *   <tr><td>5xxx</td><td>服务端内部错误</td><td>INTERNAL_ERROR(5001), SERVICE_UNAVAILABLE(5002)</td></tr>
 * </table>
 * <p>
 * <b>扩展指南：</b> 各业务模块可自行创建实现 {@link ErrorCode} 的枚举类，
 * 建议使用不同的号段避免与通用错误码冲突（例如资产服务使用 3xxx，扫描服务使用 4xxx）。
 *
 * @see ErrorCode
 * @see ApiException
 * @see ApiResponse
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

    /** 业务错误码（自定义，非 HTTP 状态码） */
    private final int code;
    /** 面向用户的错误提示信息 */
    private final String message;
    /** 对应的 HTTP 状态码数值 */
    private final int httpCode;

    CommonErrorCode(int code, String message, int httpCode) {
        this.code = code;
        this.message = message;
        this.httpCode = httpCode;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getHttpCode() {
        return httpCode;
    }

    /**
     * 将内部的 HTTP 状态码数值转换为 Spring 的 {@link HttpStatus} 枚举。
     * <p>
     * 注意：{@link HttpStatus#valueOf(int)} 仅支持标准的 HTTP 状态码（100-599），
     * 若传入非标准值会抛出 {@link IllegalArgumentException}。
     */
    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.valueOf(httpCode);
    }
}
