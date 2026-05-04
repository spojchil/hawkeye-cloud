package com.hawkeye.detection.common.enums;

import lombok.Getter;

/**
 * 检测结果状态枚举。
 * <p>
 * 数据库存 VARCHAR，值域：matched / not_matched / error
 */
@Getter
public enum DetectionStatusEnum {

    MATCHED("matched", "命中"),
    NOT_MATCHED("not_matched", "未命中"),
    ERROR("error", "错误");

    private final String value;
    private final String description;

    DetectionStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 根据 value 查找枚举。
     */
    public static DetectionStatusEnum fromValue(String value) {
        for (DetectionStatusEnum status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的检测状态: " + value);
    }
}
