package com.common.utils.response;

import org.springframework.http.HttpStatus;

/**
 * 错误类型接口
 */
public interface ErrorCode {

    int getCode();

    String getMessage();

    /**
     * 返回 HTTP 状态码
     * 这里返回 int 为了兼容性
     */
    default int getHttpCode() {
        return HttpStatus.BAD_REQUEST.value();
    }

    /**
     * 默认方法 - 返回 HttpStatus
     */
    default HttpStatus getHttpStatus() {
        return HttpStatus.valueOf(getHttpCode());
    }
}
