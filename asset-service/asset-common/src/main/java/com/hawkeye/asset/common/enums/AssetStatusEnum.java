package com.hawkeye.asset.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum AssetStatusEnum implements IEnum<Integer> {
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
    public Integer getValue() {
        return this.code;
    }

    public String getDesc() {
        return this.desc;
    }
}
