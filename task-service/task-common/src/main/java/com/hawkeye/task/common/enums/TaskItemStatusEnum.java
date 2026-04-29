package com.hawkeye.task.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum TaskItemStatusEnum implements IEnum<Integer> {

    PENDING(0, "待执行"),
    SUCCESS(1, "成功"),
    NO_MATCH(2, "未匹配"),
    FAILED(3, "失败");

    @EnumValue
    private final Integer value;

    @JsonValue
    private final String description;

    TaskItemStatusEnum(Integer value, String description) {
        this.value = value;
        this.description = description;
    }
}
