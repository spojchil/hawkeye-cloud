package com.common.utils.response;

import org.springframework.http.HttpStatus;

/**
 * 错误码接口
 */
public interface ErrorCode {

    int getCode();

    String getMessage();

    default int getHttpCode() {
        return HttpStatus.BAD_REQUEST.value();
    }

    default HttpStatus getHttpStatus() {
        return HttpStatus.valueOf(getHttpCode());
    }
}
