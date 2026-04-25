package com.hawkeye.asset.common.enums;

import com.common.utils.enums.CodeEnum;

public enum AssetStatusEnum implements CodeEnum {
    DISABLED(0, "禁用"),
    ENABLED(1, "启用"),
    DEPRECATED(2, "弃用");

    private final int code;
    private final String desc;

    AssetStatusEnum(int code, String desc) {
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
