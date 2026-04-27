package com.hawkeye.asset.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum RequestMethodEnum implements IEnum<Integer> {
    GET(0, "GET"),
    HEAD(1, "HEAD"),
    POST(2, "POST"),
    PUT(3, "PUT"),
    PATCH(4, "PATCH"),
    DELETE(5, "DELETE"),
    OPTIONS(6, "OPTIONS"),
    TRACE(7, "TRACE");

    private final int code;
    private final String desc;

    RequestMethodEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.code;
    }

    public String getDesc() {
        return this.desc;
    }
}