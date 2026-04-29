package com.hawkeye.detection.common.enums;

/**
 * 检测结果状态枚举。
 * <p>
 * 数据库存 TINYINT，通过 MybatisEnumTypeHandler 自动映射。
 */
public enum DetectionStatusEnum {
    SUCCESS("成功"),   // HTTP 探测完成 + 匹配命中
    FAILED("失败"),    // HTTP 探测完成 + 未命中
    ERROR("错误"),     // 探测过程异常（超时、DNS 失败等）
    TIMEOUT("超时"),   // HTTP 请求超时
    UNKNOWN("未知");   // 未执行或其他

    private final String desc;

    DetectionStatusEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return this.desc;
    }
}
