package com.hawkeye.task.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 任务状态枚举。
 * <p>
 * 状态流转: PENDING → RUNNING → DONE / ERROR / CANCELLED
 * <p>
 * 值必须与 DDL 注释一致：0=待执行, 1=执行中, 2=已完成, 3=已取消, 4=异常终止
 */
@Getter
public enum TaskStatusEnum implements IEnum<Integer> {

    PENDING(0, "待执行"),
    RUNNING(1, "执行中"),
    DONE(2, "已完成"),
    CANCELLED(3, "已取消"),
    ERROR(4, "异常终止");

    @EnumValue
    private final Integer value;

    @JsonValue
    private final String description;

    TaskStatusEnum(Integer value, String description) {
        this.value = value;
        this.description = description;
    }
}
