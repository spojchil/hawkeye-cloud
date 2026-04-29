package com.hawkeye.detection.common.enums;

/**
 * 检测结果状态枚举。
 * <p>
 * 数据库存 VARCHAR，通过 name() 序列化（如 "SUCCESS" / "FAILED"）。
 * detection_result 表 status 字段为 VARCHAR(20)，与实体 String status 对应。
 */
public enum DetectionStatusEnum {
    SUCCESS("成功"),
    FAILED("失败"),
    ERROR("错误"),
    TIMEOUT("超时"),
    UNKNOWN("未知");

    private final String desc;

    DetectionStatusEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return this.desc;
    }
}
