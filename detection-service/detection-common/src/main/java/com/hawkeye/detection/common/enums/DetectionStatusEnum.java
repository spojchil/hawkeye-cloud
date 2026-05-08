package com.hawkeye.detection.common.enums;

/**
 * 检测结果状态
 */
public enum DetectionStatusEnum {

    MATCHED("matched"),
    NOT_MATCHED("not_matched"),
    ERROR("error");

    private final String value;

    DetectionStatusEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
