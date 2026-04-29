package com.hawkeye.vul.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 漏洞严重程度枚举，映射 Nuclei 模板的 severity 字段。
 * 实现 IEnum 以配合 MybatisEnumTypeHandler 自动与 DB tinyint 互转。
 */
public enum VulSeverityEnum implements IEnum<Integer> {
    INFO(0, "信息"),
    LOW(1, "低危"),
    MEDIUM(2, "中危"),
    HIGH(3, "高危"),
    CRITICAL(4, "严重"),
    UNKNOWN(5, "未知");

    private final int code;
    private final String desc;

    VulSeverityEnum(int code, String desc) {
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

    public static VulSeverityEnum fromNucleiSeverity(String severity) {
        if (severity == null) {
            return UNKNOWN;
        }
        return switch (severity.toLowerCase()) {
            case "info" -> INFO;
            case "low" -> LOW;
            case "medium" -> MEDIUM;
            case "high" -> HIGH;
            case "critical" -> CRITICAL;
            default -> UNKNOWN;
        };
    }
}
