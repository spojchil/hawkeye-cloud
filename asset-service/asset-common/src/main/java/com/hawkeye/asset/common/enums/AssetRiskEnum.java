package com.hawkeye.asset.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

/**
 * ★ 从自定义 CodeEnum 切换到 MyBatis-Plus IEnum
 * MyBatis-Plus 内置 MybatisEnumTypeHandler，
 * 自动识别实现了 IEnum 接口的枚举，无需再手动注册 TypeHandler。
 * 需要 在 application.yml 中配置：
 * mybatis-plus.configuration.default-enum-type-handler:
 * com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler
 */
public enum AssetRiskEnum implements IEnum<Integer> {
    UNKNOWN(0, "未知"),
    LOW(1, "低"),
    MEDIUM(2, "中"),
    HIGH(3, "高");

    private final int code;
    @Getter
    private final String desc;

    AssetRiskEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.code;
    }

}
