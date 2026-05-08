package com.hawkeye.vul.common.enums;

/**
 * 漏洞严重程度枚举，映射 Nuclei 模板 severity 字段
 *
 * <p>DB 存 VARCHAR（"low"/"high"），不需要 IEnum。
 * {@link #fromNucleiSeverity(String)} 负责 YAML 字符串转换，
 * {@link #fromDbValue(String)} 负责 DB 反序列化。</p>
 */
public enum VulSeverityEnum {
    INFO("信息"),
    LOW("低危"),
    MEDIUM("中危"),
    HIGH("高危"),
    CRITICAL("严重"),
    UNKNOWN("未知");

    private final String desc;

    VulSeverityEnum(String desc) {
        this.desc = desc;
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

    /**
     * 从 DB 反序列化
     */
    public static VulSeverityEnum fromDbValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}

