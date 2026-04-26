package com.hawkeye.asset.common.enums;

import com.common.utils.enums.CodeEnum;

public enum RequestMethodEnum implements CodeEnum {
    GET(0, "GET"),        // 获取资源
    HEAD(1, "HEAD"),       // 获取响应头
    POST(2, "POST"),       // 创建资源
    PUT(3, "PUT"),        // 更新资源
    PATCH(4, "PATCH"),      // 部分更新
    DELETE(5, "DELETE"),     // 删除资源
    OPTIONS(6, "OPTIONS"),    // 获取支持的请求方法
    TRACE(7, "TRACE");      // 追踪请求

    private final int code;
    private final String desc;

    RequestMethodEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public String getDesc() {
        return this.desc;
    }
}