package com.hawkeye.detection.common.enums;

import lombok.Getter;

/**
 * 检测结果状态
 */
@Getter
public enum DetectionStatusEnum {

    MATCHED("matched"),
    NOT_MATCHED("not_matched"),
    ERROR("error");

    private final String value;

    DetectionStatusEnum(String value) {
        this.value = value;
    }

}
