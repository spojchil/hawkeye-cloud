package com.hawkeye.task.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 检测项状态——PENDING→MATCHED/NOT_MATCHED/FAILED，预检过滤→SKIPPED，值须与 DDL 一致 (0-4)
 */
@Getter
public enum TaskItemStatusEnum implements IEnum<Integer> {

    PENDING(0, "待执行"),
    MATCHED(1, "匹配"),
    NOT_MATCHED(2, "未匹配"),
    FAILED(3, "失败"),
    SKIPPED(4, "跳过");

    @EnumValue
    private final Integer value;

    @JsonValue
    private final String description;

    TaskItemStatusEnum(Integer value, String description) {
        this.value = value;
        this.description = description;
    }
}
