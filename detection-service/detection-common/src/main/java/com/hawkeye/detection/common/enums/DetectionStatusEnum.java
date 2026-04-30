package com.hawkeye.detection.common.enums;

/**
 * 检测结果状态枚举（v2）。
 * 数据库存 VARCHAR，值域：matched / not_matched / error。
 */
public enum DetectionStatusEnum {
    MATCHED("命中"),
    NOT_MATCHED("未命中"),
    ERROR("错误");

    private final String desc;

    DetectionStatusEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return this.desc;
    }
}
