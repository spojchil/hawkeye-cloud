package com.hawkeye.asset.common.enums;

import com.common.utils.enums.CodeEnum;

public enum AssetRiskEnum implements CodeEnum {
    UNKNOWN(0, "未知"),
    LOW(1, "低"),
    MEDIUM(2, "中"),
    HIGH(3, "高");

    private final int code;
    private final String desc;

    AssetRiskEnum(int code, String desc) {
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
