package com.hawkeye.vul.common.enums;

/**
 * 漏洞严重程度枚举，映射 Nuclei 模板 severity 字段
 */
public enum VulSeverityEnum {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
    UNKNOWN;

    /**
     * 从 Nuclei YAML severity 字符串转换
     */
    public static VulSeverityEnum fromNucleiSeverity(String severity) {
        if (severity == null) return UNKNOWN;
        try {
            return valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
