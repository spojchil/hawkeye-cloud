package com.hawkeye.task.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum TaskStatusEnum implements IEnum<Integer> {

    PENDING(0, "待执行"),
    PROCESSING(1, "分发中"),
    RUNNING(2, "执行中"),
    DONE(3, "已完成"),
    CANCELLED(4, "已取消"),
    ERROR(5, "异常终止");

    @EnumValue
    private final Integer value;

    @JsonValue
    private final String description;

    TaskStatusEnum(Integer value, String description) {
        this.value = value;
        this.description = description;
    }
}
